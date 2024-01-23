/*
 * Copyright (c) YugaByte, Inc.
 */

package node

import (
	"fmt"
	"os"
	"strings"

	"github.com/sirupsen/logrus"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"github.com/yugabyte/yugabyte-db/managed/yba-cli/cmd/util"
	ybaAuthClient "github.com/yugabyte/yugabyte-db/managed/yba-cli/internal/client"
	"github.com/yugabyte/yugabyte-db/managed/yba-cli/internal/formatter"
	"github.com/yugabyte/yugabyte-db/managed/yba-cli/internal/formatter/universe"
)

var listNodeCmd = &cobra.Command{
	Use:   "list",
	Short: "List YugabyteDB Anywhere universe nodes",
	Long:  "List YugabyteDB Anywhere universe nodes",
	PreRun: func(cmd *cobra.Command, args []string) {
		universeName, err := cmd.Flags().GetString("name")
		if err != nil {
			logrus.Fatalf(formatter.Colorize(err.Error()+"\n", formatter.RedColor))
		}
		if len(strings.TrimSpace(universeName)) == 0 {
			cmd.Help()
			logrus.Fatalln(
				formatter.Colorize("No universe name found to list node instance"+
					"\n", formatter.RedColor))
		}
	},
	Run: func(cmd *cobra.Command, args []string) {
		authAPI, err := ybaAuthClient.NewAuthAPIClient()
		if err != nil {
			logrus.Fatalf(formatter.Colorize(err.Error()+"\n", formatter.RedColor))
		}
		authAPI.GetCustomerUUID()
		universeListRequest := authAPI.ListUniverses()

		universeName, err := cmd.Flags().GetString("name")
		if err != nil {
			logrus.Fatalf(formatter.Colorize(err.Error()+"\n", formatter.RedColor))
		}
		universeListRequest = universeListRequest.Name(universeName)

		r, response, err := universeListRequest.Execute()
		if err != nil {
			errMessage := util.ErrorFromHTTPResponse(
				response,
				err,
				"Node",
				"List - Fetch Universe")
			logrus.Fatalf(formatter.Colorize(errMessage.Error()+"\n", formatter.RedColor))
		}

		if len(r) < 1 {
			fmt.Println("No universe found")
			return
		}
		selectedUniverse := r[0]
		details := selectedUniverse.GetUniverseDetails()
		nodes := details.GetNodeDetailsSet()

		NodeCtx := formatter.Context{
			Output: os.Stdout,
			Format: universe.NewNodesFormat(viper.GetString("output")),
		}
		if len(nodes) < 1 {
			fmt.Println("No universe node instances found")
			return
		}
		universe.NodeWrite(NodeCtx, nodes)

	},
}

func init() {
	listNodeCmd.Flags().SortFlags = false
}
