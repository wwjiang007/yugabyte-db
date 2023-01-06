// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.

#include <sstream>
#include <thread>

#include <boost/algorithm/string.hpp>
#include <boost/optional.hpp>
#include <boost/asio/ip/tcp.hpp>
#include <rapidjson/reader.h>
#include <rapidjson/writer.h>

#include "yb/rpc/scheduler.h"

#include "yb/server/call_home.h"

#include "yb/util/net/net_fwd.h"
#include "yb/util/flags.h"
#include "yb/util/jsonwriter.h"
#include "yb/util/logging.h"
#include "yb/util/metrics.h"
#include "yb/util/result.h"
#include "yb/util/user.h"
#include "yb/util/version_info.h"

using std::string;
using std::vector;

static const char* kLowLevel = "low";
static const char* kMediumLevel = "medium";
static const char* kHighLevel = "high";

DEFINE_RUNTIME_bool(callhome_enabled, true,
    "Enables callhome feature that sends analytics data to yugabyte");

DEFINE_RUNTIME_int32(callhome_interval_secs, 3600, "How often to run callhome");
// TODO: We need to change this to https, it involves updating our libcurl
// implementation to support SSL.
DEFINE_RUNTIME_string(callhome_url, "http://diagnostics.yugabyte.com", "URL of callhome server");
DEFINE_RUNTIME_string(callhome_collection_level, kMediumLevel, "Level of details sent by callhome");

DEFINE_RUNTIME_string(callhome_tag, "",
    "Tag to be inserted in the json sent to FLAGS_callhome_url. "
    "This tag is used by itest to specify that the data generated by "
    "callhome should be discarded by the receiver.");

using google::CommandlineFlagsIntoString;
using strings::Substitute;
using yb::server::RpcAndWebServerBase;

namespace yb {

Collector::~Collector() {}

Collector::Collector(RpcAndWebServerBase* server) : server_(server) {}

bool Collector::Run(CollectionLevel level) {
  json_.clear();
  if (collection_level() <= level) {
    Collect(level);
    return true;
  } else {
    LOG(INFO) << "Skipping collector " << collector_name()
              << " because it has a higher collection level than the requested one";
  }

  return false;
}

void Collector::AppendPairToJson(
    const std::string& key, const std::string& value, std::string* out) {
  if (!out->empty()) {
    *out += ",";
  }
  *out += '\"';
  *out += key;
  *out += "\":\"";
  *out += value;
  *out += '\"';
}

std::string GetCurrentUser() {
  auto user_name = GetLoggedInUser();
  if (user_name.ok()) {
    YB_LOG_FIRST_N(INFO, 1) << "Logged in user: " << *user_name;
    return *user_name;
  } else {
    YB_LOG_FIRST_N(WARNING, 1) << "Failed to get current user: " << user_name.status();
    return "unknown_user";
  }
}

class MetricsCollector : public Collector {
 public:
  using Collector::Collector;

  void Collect(CollectionLevel collection_level) {
    std::stringstream s;
    JsonWriter w(&s, JsonWriter::COMPACT);
    MetricEntityOptions entity_opts;
    entity_opts.metrics.push_back("*");
    Status status = server_->metric_registry()->WriteAsJson(&w, entity_opts, MetricJsonOptions());
    if (!status.ok()) {
      json_ = "\"metrics\":{}";
      return;
    }
    json_ = "\"metrics\":" + s.str();
  }

  string collector_name() { return "MetricsCollector"; }

  virtual CollectionLevel collection_level() { return CollectionLevel::HIGH; }
};

class RpcsCollector : public Collector {
 public:
  using Collector::Collector;

  void Collect(CollectionLevel collection_level) {
    if (!UpdateAddr().ok()) {
      json_ = "\"rpcs\":{}";
      return;
    }

    faststring buf;
    auto url = Substitute("http://$0/rpcz", yb::ToString(*addr_));
    auto status = curl_.FetchURL(url, &buf);
    if (!status.ok()) {
      LOG(ERROR) << "Unable to read url " << url;
      return;
    }

    if (buf.length() > 0) {
      auto rpcs_json = buf.ToString();
      boost::replace_all(rpcs_json, "\n", "");
      json_ = "\"rpcs\":" + rpcs_json;
    } else {
      LOG(WARNING) << "Error getting rpcs";
    }
  }

  string collector_name() { return "RpcsCollector"; }

  virtual CollectionLevel collection_level() { return CollectionLevel::HIGH; }

 private:
  Status UpdateAddr() {
    if (addr_) {
      return Status::OK();
    }

    vector<Endpoint> addrs;
    auto status = server_->web_server()->GetBoundAddresses(&addrs);
    if (!status.ok()) {
      LOG(WARNING) << "Unable to get webserver address: " << status.ToString();
      return STATUS(InternalError, "Unable to get webserver address");
    }
    addr_.emplace(addrs[0]);
    return Status::OK();
  }

  boost::optional<Endpoint> addr_;
  EasyCurl curl_;
};

class GFlagsCollector : public Collector {
 public:
  using Collector::Collector;

  void Collect(CollectionLevel collection_level) {
    auto gflags = CommandlineFlagsIntoString();
    boost::replace_all(gflags, "\n", " ");
    string escaped_gflags;
    JsonEscape(gflags, &escaped_gflags);
    json_ = Substitute("\"gflags\":\"$0\"", escaped_gflags);
  }

  string collector_name() { return "GFlagsCollector"; }

  virtual CollectionLevel collection_level() { return CollectionLevel::LOW; }
};

CallHome::CallHome(server::RpcAndWebServerBase* server) : server_(server), pool_("call_home", 1) {
  scheduler_ = std::make_unique<yb::rpc::Scheduler>(&pool_.io_service());

  AddCollector<MetricsCollector>();
  AddCollector<RpcsCollector>();
  AddCollector<GFlagsCollector>();
}

CallHome::~CallHome() {
  scheduler_->Shutdown();
  pool_.Shutdown();
  pool_.Join();
}

std::string CallHome::BuildJson() {
  string str = "{";
  string comma = "";
  auto collection_level = GetCollectionLevel();
  for (const auto& collector : collectors_) {
    if (collector->Run(collection_level) && !collector->as_json().empty()) {
      str += comma;
      str += collector->as_json();
      comma = ",";
      LOG(INFO) << "Done with collector " << collector->collector_name();
    }
  }
  if (!FLAGS_callhome_tag.empty()) {
    str += comma;
    str += Substitute("\"tag\":\"$0\"", FLAGS_callhome_tag);
  }
  str += "}";

  rapidjson::StringBuffer buffer;
  rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
  rapidjson::Reader reader;
  rapidjson::StringStream ss(str.c_str());
  if (!reader.Parse<rapidjson::kParseDefaultFlags>(ss, writer)) {
    LOG(ERROR) << "Unable to parse json. Error: " << reader.GetParseErrorCode() << " at offset "
               << reader.GetErrorOffset() << " in string "
               << str.substr(reader.GetErrorOffset(), 10);
    return str;
  }

  return buffer.GetString();
}

void CallHome::BuildJsonAndSend() {
  auto json = BuildJson();
  SendData(json);
}

void CallHome::DoCallHome() {
  if (FLAGS_callhome_enabled) {
    if (!SkipCallHome()) {
      BuildJsonAndSend();
    }
  }

  ScheduleCallHome(FLAGS_callhome_interval_secs);
}

void CallHome::SendData(const string& payload) {
  faststring reply;

  auto status = curl_.PostToURL(FLAGS_callhome_url, payload, "application/json", &reply);
  if (!status.ok()) {
    LOG(INFO) << "Unable to send diagnostics data to " << FLAGS_callhome_url;
  }
  VLOG(1) << "Received reply: " << reply;
}

void CallHome::ScheduleCallHome(int delay_seconds) {
  scheduler_->Schedule(
      [this](const Status& status) {
        if (!status.ok()) {
          LOG(INFO) << "CallHome stopped: " << status.ToString();
          return;
        }
        DoCallHome();
      },
      std::chrono::seconds(delay_seconds));
}

CollectionLevel CallHome::GetCollectionLevel() {
  if (FLAGS_callhome_collection_level == kHighLevel) {
    return CollectionLevel::HIGH;
  } else if (FLAGS_callhome_collection_level == kMediumLevel) {
    return CollectionLevel::MEDIUM;
  } else if (FLAGS_callhome_collection_level == kLowLevel) {
    return CollectionLevel::LOW;
  }
  return CollectionLevel::LOW;
}

}  // namespace yb
