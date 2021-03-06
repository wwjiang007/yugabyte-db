// Copyright (c) YugaByte, Inc.

import React, { Component } from 'react';
import { Link } from 'react-router';
import { Row, Col } from 'react-bootstrap';
import { isFinite } from 'lodash';
import { YBLoading } from '../../common/indicators';
import { getPromiseState } from 'utils/PromiseUtils';
import { YBCost, DescriptionItem } from 'components/common/descriptors';
import { UniverseStatusContainer } from 'components/universes';
import './UniverseDisplayPanel.scss';
import { isNonEmptyObject } from "../../../utils/ObjectUtils";
import { YBModal } from '../../common/forms/fields';
import { getPrimaryCluster, getReadOnlyCluster, getClusterProviderUUIDs, getProviderMetadata } from "../../../utils/UniverseUtils";
import { isNotHidden, isDisabled } from 'utils/LayoutUtils';

const moment = require('moment');

class CTAButton extends Component {
  render() {
    const { linkTo, labelText, otherProps } = this.props;

    return (
      <Link to={linkTo}>
        <div className="create-universe-button" {...otherProps}>
          <div className="btn-icon">
            <i className="fa fa-plus"/>
          </div>
          <div className="display-name text-center">
            {labelText}
          </div>
        </div>
      </Link>
    );
  }
}

class CTAButtonAnimated extends Component {
  constructor(props) {
    super(props);
    this.state = {
      hover: false
    };

    this.plusIcon = null;
    this.mainLabel = null;
    this.addUniverseElems = [];
    this.feature = null;
    this.description = null;
    this.icons = [];
  }


  mouseEnter = () => {
    this.setState({hover: true});
  }

  mouseLeave = () => {
    this.setState({hover: false});
  }

  disableLink = (e) => {
    e.preventDefault();
  }

  render() {
    const { labelText, onClick, otherProps, className, currentCustomer } = this.props;

    const createDisabled = isDisabled(currentCustomer.data.features, "universe.create");
    const importDisabled = isDisabled(currentCustomer.data.features, "universe.import");

    return (
      <div>
        <div ref={ div => this.content = div } onMouseEnter={this.mouseEnter} onMouseLeave={this.mouseLeave} className={className ? "create-universe-button "+className : "create-universe-button"} {...otherProps}>
          <div ref={ div => this.plusIcon = div } className="btn-icon">
            <i className="fa fa-plus"/>
          </div>
          <div ref={ div => this.mainLabel = div } className="display-name text-center">
            {labelText}
          </div>
          <div className="button-group">
            {isNotHidden(currentCustomer.data.features, "universe.create") &&
            <Link className="btn btn-default" to={"/universes/create"}
              disabled={createDisabled}
              onClick={createDisabled ? this.disableLink: onClick}>
              <div ref={ block => this.addUniverseElems[0] = block }>
                <span className="fa fa-plus"></span> Create Universe
              </div>
            </Link>}
            {isNotHidden(currentCustomer.data.features, "universe.import") &&
            <Link to={"/importer"} className="btn btn-default"
              disabled={importDisabled}
              onClick={importDisabled ? this.disableLink: onClick}>
              <div ref={ block => this.addUniverseElems[1] = block }>
                <span className="fa fa-mail-forward"></span> Import Universe
              </div>
            </Link>}
          </div>
        </div>
      </div>
    );
  }
}

class UniverseDisplayItem extends Component {
  render() {
    const {universe, providers, refreshUniverseData} = this.props;
    if (!isNonEmptyObject(universe)) {
      return <span/>;
    }
    const primaryCluster = getPrimaryCluster(universe.universeDetails.clusters);
    if (!isNonEmptyObject(primaryCluster) || !isNonEmptyObject(primaryCluster.userIntent)) {
      return <span/>;
    }
    const readOnlyCluster = getReadOnlyCluster(universe.universeDetails.clusters);
    const clusterProviderUUIDs = getClusterProviderUUIDs(universe.universeDetails.clusters);
    const clusterProviders = providers.data.filter((p) => clusterProviderUUIDs.includes(p.uuid));
    const replicationFactor = <span>{`${primaryCluster.userIntent.replicationFactor}`}</span>;
    const universeProviders = clusterProviders.map((provider) => {
      return getProviderMetadata(provider).name;
    });
    const universeProviderText = universeProviders.join(", ");
    let nodeCount = primaryCluster.userIntent.numNodes;
    if (isNonEmptyObject(readOnlyCluster)) {
      nodeCount += readOnlyCluster.userIntent.numNodes;
    }
    const numNodes = <span>{nodeCount}</span>;
    let costPerMonth = <span>n/a</span>;
    if (isFinite(universe.pricePerHour)) {
      costPerMonth = <YBCost value={universe.pricePerHour} multiplier={"month"}/>;
    }
    const universeCreationDate = universe.creationDate ? moment(Date.parse(universe.creationDate), "x").format("MM/DD/YYYY") : "";
    return (
      <Col sm={4} md={3} lg={2}>
        <Link to={"/universes/" + universe.universeUUID}>
          <div className="universe-display-item-container">
            <div className="status-icon">
              <UniverseStatusContainer currentUniverse={universe} refreshUniverseData={refreshUniverseData} />
            </div>
            <div className="display-name">
              {universe.name}
            </div>
            <div className="provider-name">
              {universeProviderText}
            </div>
            <div className="description-item-list">
              <DescriptionItem title="Nodes">
                <span>{numNodes}</span>
              </DescriptionItem>
              <DescriptionItem title="Replication Factor">
                <span>{replicationFactor}</span>
              </DescriptionItem>
              <DescriptionItem title="Monthly Cost">
                <span>{costPerMonth}</span>
              </DescriptionItem>
              <DescriptionItem title="Created">
                <span>{universeCreationDate}</span>
              </DescriptionItem>
            </div>
          </div>
        </Link>
      </Col>
    );
  }
}

export default class UniverseDisplayPanel extends Component {
  constructor(props) {
    super(props);
    this.state = {
      addUniverseModal: false
    };
  }



  toggleUniverseModal = () => {
    if (!this.state.addUniverseModal) {

    }
    this.setState({addUniverseModal: !this.state.addUniverseModal});
  }
  render() {
    const self = this;
    const { universe: {universeList}, cloud: {providers}, customer: { currentCustomer } } = this.props;
    if (getPromiseState(providers).isSuccess()) {
      let universeDisplayList = <span/>;
      if (getPromiseState(universeList).isSuccess()) {
        universeDisplayList = universeList.data.sort((a, b) => {
          return Date.parse(a.creationDate) < Date.parse(b.creationDate);
        }).map(function (universeItem, idx) {
          return (<UniverseDisplayItem key={universeItem.name + idx}
                                       universe={universeItem}
                                       providers={providers}
                                       refreshUniverseData={self.props.fetchUniverseMetadata} />);
        });
      }
      const createUniverseButton =
        (<Col sm={4} md={3} lg={2}><CTAButtonAnimated
          labelText={"Add Universe"}
          className={self.state.addUniverseModal? "active" : ""}
          onClick={() => 0}
          currentCustomer={currentCustomer}
        /></Col>);
      return (
        <div className="universe-display-panel-container">
          <h2>Universes</h2>
          <Row>
            {universeDisplayList}
            {createUniverseButton}
          </Row>
          <YBModal
            className="mymodal"
            visible={self.state.addUniverseModal}
            onHide={() => this.toggleUniverseModal()}
            title={''}></YBModal>
        </div>
      );
    } else if (getPromiseState(providers).isEmpty()) {
      return (
        <div className="get-started-config">
          <span className="yb-data-name">Welcome to the <div>YugaByte Admin Console.</div></span>
          <span>Before you can create a Universe, you must configure a cloud provider.</span>
          <CTAButton
            linkTo={"config"}
            labelText={"Configure a Provider"} />
        </div>
      );
    } else {
      return <YBLoading />;
    }
  }
}
