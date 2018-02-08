/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.sp.jobmanager.core.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.analytics.permissions.PermissionProvider;
import org.wso2.carbon.cluster.coordinator.commons.node.NodeDetail;
import org.wso2.carbon.cluster.coordinator.service.ClusterCoordinator;
import org.wso2.carbon.sp.jobmanager.core.api.ApiResponseMessage;
import org.wso2.carbon.sp.jobmanager.core.api.ManagersApiService;
import org.wso2.carbon.sp.jobmanager.core.api.NotFoundException;
import org.wso2.carbon.sp.jobmanager.core.bean.ManagerConfigurationDetails;
import org.wso2.carbon.sp.jobmanager.core.dbhandler.ManagerDeploymentConfig;
import org.wso2.carbon.sp.jobmanager.core.dbhandler.StatusDashboardManagerDBHandler;
import org.wso2.carbon.sp.jobmanager.core.exception.RDBMSTableException;
import org.wso2.carbon.sp.jobmanager.core.impl.utils.Constants;
import org.wso2.carbon.sp.jobmanager.core.internal.ManagerDataHolder;
import org.wso2.carbon.sp.jobmanager.core.internal.ServiceDataHolder;
import org.wso2.carbon.sp.jobmanager.core.internal.services.DatasourceServiceComponent;
import org.wso2.carbon.sp.jobmanager.core.model.ChildApps;
import org.wso2.carbon.sp.jobmanager.core.model.Manager;
import org.wso2.carbon.sp.jobmanager.core.model.ManagerDetails;
import org.wso2.carbon.sp.jobmanager.core.model.SiddhiAppHolder;
import org.wso2.carbon.sp.jobmanager.core.topology.SiddhiTopologyCreatorImpl;
import org.wso2.carbon.sp.jobmanager.core.util.ResourceManagerConstants;
import org.wso2.msf4j.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

//import org.wso2.carbon.sp.distributed.resource.core.internal.*;
//import org.wso2.carbon.status.dashboard.core.bean.ManagerConfigurationDetails;
//import org.wso2.carbon.status.dashboard.core.dbhandler.DeploymentConfigs;
//import org.wso2.carbon.status.dashboard.core.dbhandler.StatusDashboardManagerDBHandler;
//import org.wso2.carbon.status.dashboard.core.exception.RDBMSTableException;
//import org.wso2.carbon.status.dashboard.core.impl.utils.Constants;
//import org.wso2.carbon.status.dashboard.core.internal.MonitoringDataHolder;
//import org.wso2.carbon.status.dashboard.core.internal.services.DatasourceServiceComponent;
//import org.wso2.carbon.status.dashboard.core.internal.services.PermissionGrantServiceComponent;
//import org.wso2.carbon.status.dashboard.core.model.Manager;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaMSF4JServerCodegen",
                            date = "2018-01-29T08:19:07.148Z")
@Component(service = ManagersApiService.class, immediate = true)
public class ManagersApiServiceImpl extends ManagersApiService {
    private static final Log logger = LogFactory.getLog(ManagersApiServiceImpl.class);
    private static final String MANAGER_PERMISSION_STRING = Constants.PERMISSION_APP_NAME +
            Constants.PERMISSION_SUFFIX_MANAGER;
    private PermissionProvider permissionProvider;
    private static StatusDashboardManagerDBHandler managerDashboard;
    private ManagerDeploymentConfig managerDashboardConfig;
    // private Map<String, String> userDefinedManagers = new HashMap<>();


    public ManagersApiServiceImpl() {
        managerDashboardConfig = ManagerDataHolder.getInstance().getManagerDeploymentConfig();

    }

    public static StatusDashboardManagerDBHandler getDashboardStore() { //todo: remove static
        return managerDashboard;
    }

    //    /**
//     * This is the activation method of ConfigServiceComponent. This will be called when it's references are fulfilled
//     *
//     * @throws Exception this will be thrown if an issue occurs while executing the activate method
//     */
    @Activate
    protected void start() {

        if (logger.isDebugEnabled()) {
            logger.debug("@Reference(bind) Status Dashboard ManagerApiServiceImpl API");
        }
        managerDashboard = new StatusDashboardManagerDBHandler();
    }

    private static String getUserName(Request request) {
        Object username = request.getProperty("username");
        return username != null ? username.toString() : null;
    }

    /**
     * This is the deactivation method of ConfigServiceComponent. This will be called when this component
     * is being stopped or references are satisfied during runtime.
     *
     * @throws Exception this will be thrown if an issue occurs while executing the de-activate method
     */
    @Deactivate
    protected void stop() {

        if (logger.isDebugEnabled()) {
            logger.debug("@Reference(unbind) Status Dashboard ManagerApiServiceImpl API");
        }
    }

    /**
     * Add a new manager
     *
     * @param manager  : Manager object that need to be added
     * @param username : username of the user
     * @return : Response whether the manager is successfully added or not.
     * @throws NotFoundException
     */

    @Override
    public Response addManager(Manager manager, String username) throws NotFoundException {
        //todo: need to add permission
        if (manager.getHost() != null) {
            String managerId = manager.getHost() + Constants.MANAGER_KEY_GENERATOR + String.valueOf(manager.getPort());
            ManagerConfigurationDetails managerConfigurationDetails = new ManagerConfigurationDetails(managerId,
                                                                                                      manager.getHost
                                                                                                              (),
                                                                                                      Integer.valueOf(
                                                                                                              manager.getPort()));
            StatusDashboardManagerDBHandler managerDBHandler = managerDashboard;
            try {
                managerDBHandler.insertManagerConfiguration(managerConfigurationDetails);
                logger.info("successfully added");
                return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK,
                                                                   "managerId" + managerId + "successfully added"))
                        .build();

            } catch (RDBMSTableException e) {
                logger.error("Error occured while inserting the Manager due to" + e.getMessage(), e);
                return Response.serverError().entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Error "
                        + "occured while inserting the Manager due to" + e.getMessage())).build();
            }


        } else {
            logger.error("Invalid data:" + manager.toString());
            return Response.status(Response.Status.BAD_REQUEST).entity("There is no manager nodes. please add a "
                                                                               + "manager:" + manager.toString())
                    .build();
        }


        // return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

    /**
     * @return
     * @throws NotFoundException
     */
    @Override
    public Response getAllManagers() throws NotFoundException {

        List<ManagerDetails> connectedManagers = new ArrayList<>();
        ClusterCoordinator clusterCoordinator = ServiceDataHolder.getCoordinator();

        if (clusterCoordinator != null) {
            for (NodeDetail nodeDetail : clusterCoordinator.getAllNodeDetails()) {
                if (nodeDetail.getPropertiesMap() != null) {
                    Map<String, Object> propertiesMap = nodeDetail.getPropertiesMap();
                    String httpInterfaceHost = (String) propertiesMap.get(ResourceManagerConstants.KEY_NODE_HOST);
                    int httpInterfacePort = (int) propertiesMap.get(ResourceManagerConstants.KEY_NODE_PORT);
                    String httpInterfaceUsername = (String) propertiesMap.get(
                            ResourceManagerConstants.KEY_NODE_USERNAME);
                    String httpInterfacePassword = (String) propertiesMap.get(
                            ResourceManagerConstants.KEY_NODE_PASSWORD);
                    ManagerDetails managerDetails = new ManagerDetails();

                    managerDetails.setHost(httpInterfaceHost);
                    managerDetails.setPort(httpInterfacePort);
                    managerDetails.setUsername(httpInterfaceUsername);
                    managerDetails.setPassword(httpInterfacePassword);
                    // Map<String, Object> propertiesMap = clusterCoordinator.getLeaderNode().getPropertiesMap();
                    if (clusterCoordinator.getLeaderNode().getNodeId().equals(nodeDetail.getNodeId())) {
                        managerDetails.setHaStatus("Active");
                    } else {
                        managerDetails.setHaStatus("Passive");
                    }
                    connectedManagers.add(managerDetails);
                }
            }
            return Response.ok().entity(connectedManagers).build();

        } else {
            return Response.status(Response.Status.NO_CONTENT).entity(new ApiResponseMessage(ApiResponseMessage
                                                                                                     .ERROR, "There "
                                                                                                     + "is no manager"
                                                                                                     + " nodes found "
                                                                                                     + "in the "
                                                                                                     + "cluster"))

                    .build();
        }


    }

    /**
     * We can get the details of given parent siddhi application
     * If it is in the waiting mode we can get all the details except deployed worker node details
     *
     * @param appName
     * @return
     * @throws NotFoundException
     */

    public Response getChildSiddhiApps(String appName) throws NotFoundException {

        String jsonString;

        //todo: this only displays the active node's child siddhi apps

        Map<String, List<SiddhiAppHolder>> deployedSiddhiAppHolder = ServiceDataHolder.getResourcePool()
                .getSiddhiAppHoldersMap();
        Map<String, List<SiddhiAppHolder>> waitingToDeploy = ServiceDataHolder.getResourcePool()
                .getAppsWaitingForDeploy();

        /**
         * TODO: Need to display the status of the child siddhi application.
         */

        if (waitingToDeploy.containsKey(appName)) {
            List<SiddhiAppHolder> holder = waitingToDeploy.get(appName);
            ChildApps apps = new ChildApps();
            apps.setSiddhiContent(holder);
            apps.setSiddhiAppStatus("Waiting");
            return Response.ok().entity(apps).build();
        } else {
            List<SiddhiAppHolder> activeHolder = deployedSiddhiAppHolder.get(appName);
            ChildApps childApps = new ChildApps();
            childApps.setSiddhiContent(activeHolder);
            childApps.setSiddhiAppStatus("Active");
            return Response.ok().entity(childApps).build();
        }
    }

    @Override
    public Response getChildSiddhiAppDetails(String appName, Request request) throws NotFoundException {
        //todo:check whether how can we put permission
        return getChildSiddhiApps(appName);

//        boolean isAuthorized = permissionProvider.hasPermission(username, new Permission(Constants
//         .PERMISSION_APP_NAME,

//        boolean isAuthorized =
//                permissionProvider.hasPermission(getUserName(request), new Permission(Constants.PERMISSION_APP_NAME,
//                                                                                      MANAGER_PERMISSION_STRING));
        //      if (isAuthorized) {
//            return getChildSiddhiApps(appName);
//        } else {
//            return Response.status(Response.Status.UNAUTHORIZED).entity("Insufficient permissions to get status of " +
//                                                                                "the Siddhi App " + appName).build();
//        }


    }
//
//    private PermissionProvider getPermissionProvider(){
//        return ServiceDataHolder.getPermissionProvider();
//    }

    @Override
    public Response getSiddhiApps(String id) throws NotFoundException {
//        try {
////            Gson gson=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
////            Map<String,List<SiddhiAppHolder>> holdar=ServiceDataHolder.getResourcePool().getSiddhiAppHoldersMap();
////            if(){
////
////            }
//        } catch (Exception ex) {
//
//        }
        // do some magic!
//
        SiddhiTopologyCreatorImpl siddhiTopologyCreator = new SiddhiTopologyCreatorImpl();
        //  siddhiTopologyCreator.getSiddhiAppName();
        return Response.ok().entity(siddhiTopologyCreator).build();
    }

    @Override
    public Response getTransportDEtails(String id, String appName) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

    /**
     * Delete an existing manager.
     *
     * @param managerId : id of the manager node (format : managerhost_managerport)
     * @param username  :username of the user
     * @return Response whether the manager successfully deleted or not.
     * @throws NotFoundException
     */

    @Override
    public Response deleteManager(String managerId, String username) throws NotFoundException {

        try {
            managerDashboard.deleteManagerConfiguration(managerId);
            return Response.ok()
                    .entity(new ApiResponseMessage(ApiResponseMessage.OK, managerId + "Successfully deleted"))
                    .build();
        } catch (RDBMSTableException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage
                                                                                         (ApiResponseMessage.ERROR,
                                                                                          "Error occured while "
                                                                                                  + "deleting the "
                                                                                                  +
                                                                                                  "manager" + managerId
                                                                                                  + ex.getMessage()))
                    .build();


        }
    }

    @Reference(
            name = "org.wso2.carbon.sp.jobmanager.core.internal.services.DatasourceServiceComponent",
            service = DatasourceServiceComponent.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterServiceDatasource"
    )
    public void regiterServiceDatasource(DatasourceServiceComponent datasourceServiceComponent) {

        if (logger.isDebugEnabled()) {
            logger.debug("@Reference(bind) DatasourceServiceComponent");
        }

    }

    public void unregisterServiceDatasource(DatasourceServiceComponent datasourceServiceComponent) {

        if (logger.isDebugEnabled()) {
            logger.debug("@Reference(unbind) DatasourceServiceComponent");
        }
    }

//    @Reference(
//            name = "org.wso2.carbon.status.dashboard.core.internal.services.PermissionGrantServiceComponent",
//            service = PermissionGrantServiceComponent.class,
//            cardinality = ReferenceCardinality.MANDATORY,
//            policy = ReferencePolicy.DYNAMIC,
//            unbind = "unregisterServicePermissionGrantService"
//    )
//    public void registerServicePermissionGrantService(PermissionGrantServiceComponent
// permissionGrantServiceComponent) {
//
//        if (logger.isDebugEnabled()) {
//            logger.debug("@Reference(bind) ServicePermissionGrantService");
//        }
//    }
//
//    public void unregisterServicePermissionGrantService(
//            PermissionGrantServiceComponent permissionGrantServiceComponent) {
//
//        if (logger.isDebugEnabled()) {
//            logger.debug("@Reference(unbind) ServicePermissionGrantService");
//        }
//    }


}
