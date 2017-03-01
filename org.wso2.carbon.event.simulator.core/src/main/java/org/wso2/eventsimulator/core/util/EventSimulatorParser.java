/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.eventsimulator.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.eventsimulator.core.internal.EventSimulatorDataHolder;
import org.wso2.eventsimulator.core.simulator.bean.FeedSimulationDto;
import org.wso2.eventsimulator.core.simulator.bean.FeedSimulationStreamConfiguration;
import org.wso2.eventsimulator.core.simulator.bean.FileStore;
import org.wso2.eventsimulator.core.simulator.csvFeedSimulation.CSVFileSimulationDto;
import org.wso2.eventsimulator.core.simulator.csvFeedSimulation.core.FileDto;
import org.wso2.eventsimulator.core.simulator.databaseFeedSimulation.DatabaseFeedSimulationDto;
import org.wso2.eventsimulator.core.simulator.exception.EventSimulationException;
import org.wso2.eventsimulator.core.simulator.randomdatafeedsimulation.bean.CustomBasedAttribute;
import org.wso2.eventsimulator.core.simulator.randomdatafeedsimulation.bean.FeedSimulationStreamAttributeDto;
import org.wso2.eventsimulator.core.simulator.randomdatafeedsimulation.bean.PrimitiveBasedAttribute;
import org.wso2.eventsimulator.core.simulator.randomdatafeedsimulation.bean.PropertyBasedAttributeDto;
import org.wso2.eventsimulator.core.simulator.randomdatafeedsimulation.bean.RandomDataSimulationDto;
import org.wso2.eventsimulator.core.simulator.randomdatafeedsimulation.bean.RegexBasedAttributeDto;
import org.wso2.eventsimulator.core.simulator.randomdatafeedsimulation.util.RandomDataGenerator;
import org.wso2.eventsimulator.core.simulator.singleventsimulator.SingleEventDto;
import org.wso2.streamprocessor.core.StreamDefinitionRetriever;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static org.wso2.eventsimulator.core.simulator.bean.FeedSimulationStreamConfiguration.SimulationType.DATABASE_SIMULATION;
import static org.wso2.eventsimulator.core.simulator.bean.FeedSimulationStreamConfiguration.SimulationType.FILE_SIMULATION;
import static org.wso2.eventsimulator.core.simulator.bean.FeedSimulationStreamConfiguration.SimulationType.RANDOM_DATA_SIMULATION;

/**
 * EventSimulatorParser is an util class used to
 * convert Json string into relevant event simulation configuration object
 */
public class EventSimulatorParser {
    private static final Logger log = Logger.getLogger(EventSimulatorParser.class);

    /*
    Initialize EventSimulatorParser
     */
    private EventSimulatorParser() {
    }

    /**
     * Convert the RandomFeedSimulationString string into RandomDataSimulationDto Object
     * RandomRandomDataSimulationConfig can have one or more attribute simulation configuration.
     * these can be one of below types
     * 1.PRIMITIVEBASED : String/Integer/Float/Double/Boolean
     * 2.PROPERTYBASED  : this type indicates the type which generates meaning full data.
     * eg: If full name it generate meaning full name
     * 3.REGEXBASED     : this type indicates the type which generates data using given regex
     * 4.CUSTOMDATA     : this type indicates the type which generates data in given data list
     * <p>
     * Initialize RandomDataSimulationDto
     *
     * @param RandomFeedSimulationString RandomEventSimulationConfiguration String
     * @return RandomDataSimulationDto Object
     */
    private static RandomDataSimulationDto randomDataSimulatorParser(String RandomFeedSimulationString) {
        RandomDataSimulationDto randomDataSimulationDto = new RandomDataSimulationDto();

        JSONObject jsonObject = new JSONObject(RandomFeedSimulationString);
        //set properties to RandomDataSimulationDto
        //todo R 01/03/2017 do we need to check whether the json objects are null/empty or would it be checked at the point of creating the jason array
        if (jsonObject.has(EventSimulatorConstants.STREAM_NAME) && !jsonObject.getString(EventSimulatorConstants.STREAM_NAME).isEmpty()) {
            randomDataSimulationDto.setStreamName(jsonObject.getString(EventSimulatorConstants.STREAM_NAME));
        } else {
            log.error("Stream name can not be null or an empty value");
            throw new RuntimeException("Stream name can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.EXECUTION_PLAN_NAME) && !jsonObject.getString(EventSimulatorConstants.EXECUTION_PLAN_NAME).isEmpty()) {
            randomDataSimulationDto.setExecutionPlanName(jsonObject.getString(EventSimulatorConstants.EXECUTION_PLAN_NAME));
        } else {
            log.error("Execution plan name can not be null or an empty value");
            throw new RuntimeException("Execution plan name can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.EVENTS) && !jsonObject.getString(EventSimulatorConstants.EVENTS).isEmpty()) {
            if (jsonObject.getInt(EventSimulatorConstants.EVENTS) <= 0) {
                log.error("Number of events to be generated cannot be a negative value");
                throw new RuntimeException("Number of events to be generated cannot be a negative value");
            } else {
                randomDataSimulationDto.setEvents(jsonObject.getInt(EventSimulatorConstants.EVENTS));
            }
        } else {
            log.error("Number of events to be generated cannot be  null or an empty value");
            throw new RuntimeException("Number of events to be generated cannot be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.DELAY) && !jsonObject.getString(EventSimulatorConstants.DELAY).isEmpty()) {
            randomDataSimulationDto.setDelay(jsonObject.getInt(EventSimulatorConstants.DELAY));
        } else {
            log.error("Delay cannot be null or an empty value");
            throw new RuntimeException("Delay cannot be null or an empty value");
        }
        // todo R 15/02/2017 get the stream definitions here using the API
//        StreamDefinitionDto streamDefinitionDto = executionPlanDto.getInputStreamDtoMap().get(randomDataSimulationDto.getStreamName());
        LinkedHashMap<String,StreamDefinitionRetriever.Type> streamDefinition = EventSimulatorDataHolder
                .getInstance().getStreamDefinitionService().streamDefinitionService(randomDataSimulationDto.getStreamName());
        List<FeedSimulationStreamAttributeDto> feedSimulationStreamAttributeDto = new ArrayList<>();

        JSONArray jsonArray;
        if (jsonObject.has(EventSimulatorConstants.ATTRIBUTE_CONFIGURATION) && jsonObject.getJSONArray(EventSimulatorConstants.ATTRIBUTE_CONFIGURATION).length() > 0) {
            jsonArray = jsonObject.getJSONArray(EventSimulatorConstants.ATTRIBUTE_CONFIGURATION);
        } else {
            log.error("Attribute configuration cannot be null or an empty value");
            throw new RuntimeException("Attribute configuration cannot be null or an empty value");
        }

        if (jsonArray.length() != streamDefinition.size()) {
//            throw new EventSimulationException("Configuration of attributes for " +
//                    "feed simulation is missing in " + randomDataSimulationDto.getStreamName() +
//                    " : " + " No of attribute in stream " + streamDefinition.size());
            throw new EventSimulationException("Random feed simulation of stream '" + randomDataSimulationDto.getStreamName() +
                    "' requires attribute configurations for " + streamDefinition.size() + " attributes. Number of attribute" +
                    " configurations provided is " + jsonArray.length());
        }

        //convert each attribute simulation configuration as relevant objects

        Gson gson = new Gson();
        for (int i = 0; i < jsonArray.length(); i++) {
            if (!jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.RANDOMDATAGENERATORTYPE)) {
                if (jsonArray.getJSONObject(i).getString(EventSimulatorConstants.RANDOMDATAGENERATORTYPE).
                        compareTo(RandomDataGeneratorConstants.PROPERTY_BASED_ATTRIBUTE) == 0) {
                    if (!jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.PROPERTYBASEDATTRIBUTE_CATEGORY)
                            && !jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.PROPERTYBASEDATTRIBUTE_PROPERTY)) {
                        PropertyBasedAttributeDto propertyBasedAttributeDto =
                                gson.fromJson(String.valueOf(jsonArray.getJSONObject(i)), PropertyBasedAttributeDto.class);

                        feedSimulationStreamAttributeDto.add(propertyBasedAttributeDto);
                    } else {
//                        todo R 16/02/2017 specify the attribute name in simulation configuration
                        log.error("Category and property should not be null value for " +
                                RandomDataGeneratorConstants.PROPERTY_BASED_ATTRIBUTE);
                        throw new EventSimulationException("Category and property should not be null value for " +
                                RandomDataGeneratorConstants.PROPERTY_BASED_ATTRIBUTE);
                    }
                } else if (jsonArray.getJSONObject(i).getString(EventSimulatorConstants.RANDOMDATAGENERATORTYPE).
                        compareTo(RandomDataGeneratorConstants.REGEX_BASED_ATTRIBUTE) == 0) {
                    if (!jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.REGEXBASEDATTRIBUTE_PATTERN)) {
                        RegexBasedAttributeDto regexBasedAttributeDto =
                                gson.fromJson(String.valueOf(jsonArray.getJSONObject(i)), RegexBasedAttributeDto.class);
                        log.info(regexBasedAttributeDto.toString());
                        RandomDataGenerator.validateRegularExpression(regexBasedAttributeDto.getPattern());
                        feedSimulationStreamAttributeDto.add(regexBasedAttributeDto);
                    } else {
                        log.error("Pattern should not be null value for " + RandomDataGeneratorConstants.REGEX_BASED_ATTRIBUTE);
                        throw new EventSimulationException("Pattern should not be null value for " +
                                RandomDataGeneratorConstants.REGEX_BASED_ATTRIBUTE);
                    }
                } else if (jsonArray.getJSONObject(i).getString(EventSimulatorConstants.RANDOMDATAGENERATORTYPE).
                        compareTo(RandomDataGeneratorConstants.PRIMITIVE_BASED_ATTRIBUTE) == 0) {
                    if (!jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.PRIMITIVEBASEDATTRIBUTE_MIN)
                            && !jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.PRIMITIVEBASEDATTRIBUTE_MAX)
                            && !jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.PRIMITIVEBASEDATTRIBUTE_LENGTH_DECIMAL)) {
                        PrimitiveBasedAttribute primitiveBasedAttribute =
                                gson.fromJson(String.valueOf(jsonArray.getJSONObject(i)), PrimitiveBasedAttribute.class);
                        feedSimulationStreamAttributeDto.add(primitiveBasedAttribute);
                    } else {
                        log.error("Min,Max and Length value should not be null value for " +
                                RandomDataGeneratorConstants.PRIMITIVE_BASED_ATTRIBUTE);
                        throw new EventSimulationException("Min,Max and Length value should not be null value for " +
                                RandomDataGeneratorConstants.PRIMITIVE_BASED_ATTRIBUTE);
                    }
                } else if (jsonArray.getJSONObject(i).getString(EventSimulatorConstants.RANDOMDATAGENERATORTYPE).
                        compareTo(RandomDataGeneratorConstants.CUSTOM_DATA_BASED_ATTRIBUTE) == 0) {
                    if (!jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.CUSTOMDATABASEDATTRIBUTE_LIST)) {
                        CustomBasedAttribute customBasedAttribute = new CustomBasedAttribute();
                        customBasedAttribute.setType(jsonArray.getJSONObject(i).getString(EventSimulatorConstants.RANDOMDATAGENERATORTYPE));
                        customBasedAttribute.setCustomData(jsonArray.getJSONObject(i).getString(EventSimulatorConstants.CUSTOMDATABASEDATTRIBUTE_LIST));
                        feedSimulationStreamAttributeDto.add(customBasedAttribute);
                    } else {
                        // TODO: 21/12/16 only throw put stream name
                        log.error("Data list is not given for " + RandomDataGeneratorConstants.CUSTOM_DATA_BASED_ATTRIBUTE);
                        throw new EventSimulationException("Data list is not given for " + RandomDataGeneratorConstants.CUSTOM_DATA_BASED_ATTRIBUTE);
                    }
                }
            } else {
                log.error("Random Data Generator option is required  for an attribute : " +
                        RandomDataGeneratorConstants.PROPERTY_BASED_ATTRIBUTE + "/" +
                        RandomDataGeneratorConstants.REGEX_BASED_ATTRIBUTE + "/" + RandomDataGeneratorConstants.PRIMITIVE_BASED_ATTRIBUTE +
                        "/" + RandomDataGeneratorConstants.CUSTOM_DATA_BASED_ATTRIBUTE);
                throw new EventSimulationException("Random Data Generator option is required  for an attribute : " +
                        RandomDataGeneratorConstants.PROPERTY_BASED_ATTRIBUTE + "/" +
                        RandomDataGeneratorConstants.REGEX_BASED_ATTRIBUTE + "/" + RandomDataGeneratorConstants.PRIMITIVE_BASED_ATTRIBUTE +
                        "/" + RandomDataGeneratorConstants.CUSTOM_DATA_BASED_ATTRIBUTE);
            }
        }
        randomDataSimulationDto.setFeedSimulationStreamAttributeDto(feedSimulationStreamAttributeDto);

        return randomDataSimulationDto;
    }


    /**
     * Convert the singleEventSimulationConfigurationString string into SingleEventDto Object
     * Initialize SingleEventDto
     *
     * @param singleEventSimulationConfigurationString singleEventSimulationConfigurationString String
     * @return SingleEventDto Object
     */
    public static SingleEventDto singleEventSimulatorParser(String singleEventSimulationConfigurationString) {
        SingleEventDto singleEventDto;
        ObjectMapper mapper = new ObjectMapper();
        //Convert the singleEventSimulationConfigurationString string into SingleEventDto Object
//        todo R 01/03/2017 check whether it works with the execution plan name
        try {
            singleEventDto = mapper.readValue(singleEventSimulationConfigurationString, SingleEventDto.class);
            singleEventDto.setSimulationType(FeedSimulationStreamConfiguration.SimulationType.SINGLE_EVENT);
            singleEventDto.setTimestampAttribute(null);

            LinkedHashMap<String,StreamDefinitionRetriever.Type> streamDefinition = EventSimulatorDataHolder
                    .getInstance().getStreamDefinitionService().streamDefinitionService(singleEventDto.getStreamName());
            if (singleEventDto.getAttributeValues().size() != streamDefinition.size()) {
                log.error("Number of attribute values is not equal to number of attributes in stream '" +
                        singleEventDto.getStreamName() + "' . Required number of attributes : " +
                        streamDefinition.size());
                throw new EventSimulationException("Number of attribute values is not equal to number of attributes in stream '" +
                        singleEventDto.getStreamName() + "' . Required number of attributes : " +
                        streamDefinition.size());
            }
        } catch (IOException e) {
            log.error("Exception occurred when parsing json to Object ");
            throw new EventSimulationException("Exception occurred when parsing json to Object : " + e.getMessage());
        }
        return singleEventDto;
    }

    /**
     * Convert the csvFileDetail string into CSVFileSimulationDto Object
     * <p>
     * Initialize CSVFileSimulationDto
     * Initialize FileStore
     *
     * @param csvFileDetail csvFileDetail String
     * @return CSVFileSimulationDto Object
     */
    private static CSVFileSimulationDto fileFeedSimulatorParser(String csvFileDetail) {
        CSVFileSimulationDto csvFileSimulationDto = new CSVFileSimulationDto();
        FileStore fileStore = FileStore.getFileStore();

        JSONObject jsonObject = new JSONObject(csvFileDetail);
        if (jsonObject.has(EventSimulatorConstants.STREAM_NAME) && !jsonObject.getString(EventSimulatorConstants.STREAM_NAME).isEmpty()) {
            csvFileSimulationDto.setStreamName(jsonObject.getString(EventSimulatorConstants.STREAM_NAME));
        } else {
            log.error("Stream name can not be null or an empty value");
            throw new RuntimeException("Stream name can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.EXECUTION_PLAN_NAME) && !jsonObject.getString(EventSimulatorConstants.EXECUTION_PLAN_NAME).isEmpty()) {
            csvFileSimulationDto.setExecutionPlanName(jsonObject.getString(EventSimulatorConstants.EXECUTION_PLAN_NAME));
        } else {
            log.error("Execution plan name can not be null or an empty value");
            throw new RuntimeException("Stream name can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.FILE_NAME) && !jsonObject.getString(EventSimulatorConstants.FILE_NAME).isEmpty()) {
            csvFileSimulationDto.setFileName(jsonObject.getString(EventSimulatorConstants.FILE_NAME));
        } else {
            log.error("File name can not be null or an empty value");
            throw new RuntimeException("File name can not be null or an empty value");
        }
        //get the fileDto from FileStore if file exist and set this value.
        FileDto fileDto;
        try {
            if (fileStore.checkExists(csvFileSimulationDto.getFileName())) {
                fileDto = fileStore.getFileInfoMap().get(csvFileSimulationDto.getFileName());
            } else {
                // TODO: 21/12/16 file name is not provided
                log.error("File does not Exist : " + csvFileSimulationDto.getFileName());
                throw new EventSimulationException("File does not Exist");
            }
            csvFileSimulationDto.setFileDto(fileDto);
            csvFileSimulationDto.setDelimiter((String) jsonObject.get(EventSimulatorConstants.DELIMITER));
            csvFileSimulationDto.setDelay(jsonObject.getInt(EventSimulatorConstants.DELAY));


        } catch (Exception FileNotFound) {
            System.out.println("File not found : " + FileNotFound.getMessage());
        }
        return csvFileSimulationDto;
    }
    // TODO R database parser

    /**
     * Convert the database configuration file into a DatabaseFeedSimulationDto object
     *
     * @param databaseConfigurations : database configuration string
     * @return a DatabaseFeedSimulationDto object
     */

    private static DatabaseFeedSimulationDto databaseFeedSimulationParser(String databaseConfigurations) {

        DatabaseFeedSimulationDto databaseFeedSimulationDto = new DatabaseFeedSimulationDto();
        JSONObject jsonObject = new JSONObject(databaseConfigurations);

//       assign values for database configuration attributes


        if (jsonObject.has(EventSimulatorConstants.DATABASE_CONFIGURATION_NAME) && !jsonObject.getString(EventSimulatorConstants.DATABASE_CONFIGURATION_NAME).isEmpty()) {
            databaseFeedSimulationDto.setDatabaseConfigName(jsonObject.getString(EventSimulatorConstants.DATABASE_CONFIGURATION_NAME));
        } else {
            log.error("Database configuration name can not be null or an empty value");
            throw new RuntimeException("Database configuration name can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.DATABASE_NAME) && !jsonObject.getString(EventSimulatorConstants.DATABASE_NAME).isEmpty()) {
            databaseFeedSimulationDto.setDatabaseName(jsonObject.getString(EventSimulatorConstants.DATABASE_NAME));
        } else {
            log.error("Database name can not be null or an empty value");
            throw new RuntimeException("Database name can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.USER_NAME) && !jsonObject.getString(EventSimulatorConstants.USER_NAME).isEmpty()) {
            databaseFeedSimulationDto.setUsername(jsonObject.getString(EventSimulatorConstants.USER_NAME));
        } else {
            log.error("Username can not be null or an empty value");
            throw new RuntimeException("Username can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.PASSWORD) && !jsonObject.getString(EventSimulatorConstants.PASSWORD).isEmpty()) {
            databaseFeedSimulationDto.setPassword(jsonObject.getString(EventSimulatorConstants.PASSWORD));
        } else {
            log.error("Password can not be null or an empty value");
            throw new RuntimeException("Password can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.TABLE_NAME) && !jsonObject.getString(EventSimulatorConstants.TABLE_NAME).isEmpty()) {
            databaseFeedSimulationDto.setTableName(jsonObject.getString(EventSimulatorConstants.TABLE_NAME));
        } else {
            log.error("Table name can not be null or an empty value");
            throw new RuntimeException("Table name can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.STREAM_NAME) && !jsonObject.getString(EventSimulatorConstants.STREAM_NAME).isEmpty()) {
            databaseFeedSimulationDto.setStreamName(jsonObject.getString(EventSimulatorConstants.STREAM_NAME));
        } else {
            log.error("Stream name can not be null or an empty value");
            throw new RuntimeException("Stream name can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.EXECUTION_PLAN_NAME) && !jsonObject.getString(EventSimulatorConstants.EXECUTION_PLAN_NAME).isEmpty()) {
            databaseFeedSimulationDto.setExecutionPlanName(jsonObject.getString(EventSimulatorConstants.EXECUTION_PLAN_NAME));
        } else {
            log.error("Execution plan name can not be null or an empty value");
            throw new RuntimeException("Execution plan name can not be null or an empty value");
        }
        if (jsonObject.has(EventSimulatorConstants.DELAY) && !jsonObject.getString(EventSimulatorConstants.DELAY).isEmpty()) {
            databaseFeedSimulationDto.setDelay(jsonObject.getInt(EventSimulatorConstants.DELAY));
        } else {
            log.error("Delay can not be null or an empty value");
            throw new RuntimeException("Delay can not be null or an empty value");
        }

        JSONArray jsonArray = jsonObject.getJSONArray(EventSimulatorConstants.COLUMN_NAMES_AND_TYPES);

        LinkedHashMap<String, String> columnAndTypes = new LinkedHashMap<>();

//       insert the specified column names and types into a hashmap and insert it to database configuration
        for (int i = 0; i < jsonArray.length(); i++) {
            if (!jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.COLUMN_NAME) &&
                    !jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.COLUMN_TYPE)) {

                columnAndTypes.put(jsonArray.getJSONObject(i).getString(EventSimulatorConstants.COLUMN_NAME),
                        jsonArray.getJSONObject(i).getString(EventSimulatorConstants.COLUMN_TYPE));
                System.out.println(columnAndTypes.entrySet());
            } else {
                throw new EventSimulationException("Column name and type cannot contain null values");
            }
        }

        databaseFeedSimulationDto.setColumnNamesAndTypes(columnAndTypes);

        return databaseFeedSimulationDto;
    }


    /**
     * Convert the feedSimulationDetails string into FeedSimulationDto Object
     * Three types of feed simulation are applicable for an input stream
     * These types are
     * 1. CSV file feed simulation : Simulate using CSV File
     * 2. Database Simulation : Simulate using Database source
     * 3. Random data simulation : Simulate using Generated random Data
     * <p>
     * Initialize FeedSimulationDto
     *
     * @param feedSimulationDetails feedSimulationDetails
     * @return FeedSimulationDto Object
     */
    public static FeedSimulationDto feedSimulationParser(String feedSimulationDetails) {

            FeedSimulationDto feedSimulationDto = new FeedSimulationDto();
            JSONObject jsonObject = new JSONObject(feedSimulationDetails);

            List<FeedSimulationStreamConfiguration> feedSimulationStreamConfigurationList = new ArrayList<>();

            JSONArray jsonArray = jsonObject.getJSONArray(EventSimulatorConstants.FEED_SIMULATION_STREAM_CONFIGURATION);

            if (jsonObject.getBoolean(EventSimulatorConstants.ORDER_BY_TIMESTAMP)) {
                feedSimulationDto.setOrderByTimeStamp(jsonObject.getBoolean(EventSimulatorConstants.ORDER_BY_TIMESTAMP));
                feedSimulationDto.setNoOfParallelSimulationSources(jsonArray.length());
                EventSender.getInstance().setMinQueueSize(2 * (feedSimulationDto.getNoOfParallelSimulationSources()));
            }

            //check the simulation type for databaseFeedSimulation given stream and convert the string to relevant configuration object
            //            1. CSV file feed simulation : Simulate using CSV File
            //            2. Database Simulation : Simulate using Database source
            //            3. Random data simulation : Simulate using random Data generated

            for (int i = 0; i < jsonArray.length(); i++) {
                if (!jsonArray.getJSONObject(i).isNull(EventSimulatorConstants.FEED_SIMULATION_TYPE)
                        && !jsonArray.getJSONObject(i).getString(EventSimulatorConstants.FEED_SIMULATION_TYPE).isEmpty()) {
                    FeedSimulationStreamConfiguration.SimulationType feedSimulationType = FeedSimulationStreamConfiguration.SimulationType.valueOf
                            (jsonArray.getJSONObject(i).getString(EventSimulatorConstants.FEED_SIMULATION_TYPE).toUpperCase());

                    switch (feedSimulationType) {
                        case RANDOM_DATA_SIMULATION:
                            RandomDataSimulationDto randomDataSimulationDto =
                                    randomDataSimulatorParser(String.valueOf(jsonArray.getJSONObject(i)));
                            randomDataSimulationDto.setSimulationType(RANDOM_DATA_SIMULATION);
                            feedSimulationStreamConfigurationList.add(randomDataSimulationDto);
                            break;
                        case FILE_SIMULATION:
                            CSVFileSimulationDto csvFileConfig = EventSimulatorParser.
                                    fileFeedSimulatorParser(String.valueOf(jsonArray.getJSONObject(i)));
                            if (feedSimulationDto.getOrderByTimeStamp()) {
                                if (jsonArray.getJSONObject(i).has(EventSimulatorConstants.TIMESTAMP_POSITION) &&
                                        !(jsonArray.getJSONObject(i).getString(EventSimulatorConstants.TIMESTAMP_POSITION).isEmpty())) {
                                    csvFileConfig.setTimestampAttribute(String.valueOf(jsonArray.getJSONObject(i).
                                            getString(EventSimulatorConstants.TIMESTAMP_POSITION)));
                                } else {
                                    csvFileConfig.setTimestampAttribute(String.valueOf(1));
                                }
                            }
                            csvFileConfig.setSimulationType(FeedSimulationStreamConfiguration.SimulationType.FILE_SIMULATION);
                            //streamConfigurationListMap.put(csvFileConfig.getStreamName(),csvFileConfig);
                            feedSimulationStreamConfigurationList.add(csvFileConfig);
                            break;
                        // TODO: 20/12/16 database

                        case DATABASE_SIMULATION:
                            DatabaseFeedSimulationDto databaseFeedSimulationDto =
                                    EventSimulatorParser.databaseFeedSimulationParser(String.valueOf(jsonArray.getJSONObject(i)));
                            if (feedSimulationDto.getOrderByTimeStamp()) {
                                if (jsonArray.getJSONObject(i).has(EventSimulatorConstants.TIMESTAMP_ATTRIBUTE) &&
                                        !(jsonArray.getJSONObject(i).getString(EventSimulatorConstants.TIMESTAMP_ATTRIBUTE).isEmpty())) {
                                    databaseFeedSimulationDto.setTimestampAttribute(String.valueOf(jsonArray.getJSONObject(i).
                                            getString(EventSimulatorConstants.TIMESTAMP_ATTRIBUTE)));
                                } else {
                                    databaseFeedSimulationDto.setTimestampAttribute(String.valueOf(databaseFeedSimulationDto.
                                            getColumnNamesAndTypes().keySet().toArray()[0]));
                                }
                            }
                            databaseFeedSimulationDto.setSimulationType(FeedSimulationStreamConfiguration.SimulationType.DATABASE_SIMULATION);
                            feedSimulationStreamConfigurationList.add(databaseFeedSimulationDto);

                            break;
//                            todo r 01/03/2017 does the event simulator constants below need to be replace with the enum?
                        default:
                            log.error(feedSimulationType + " is not available , required only : " +
                                    EventSimulatorConstants.RANDOM_DATA_SIMULATION
                                    + " or " + EventSimulatorConstants.FILE_FEED_SIMULATION + " or " +
                                    EventSimulatorConstants.DATABASE_FEED_SIMULATION);
                            throw new EventSimulationException(feedSimulationType + " is not available , required only : " +
                                    EventSimulatorConstants.RANDOM_DATA_SIMULATION
                                    + " or " + EventSimulatorConstants.FILE_FEED_SIMULATION + " or " +
                                    EventSimulatorConstants.DATABASE_FEED_SIMULATION);
                    }
                } else {
                    log.error(EventSimulatorConstants.FEED_SIMULATION_TYPE + " is null");
                    throw new EventSimulationException(EventSimulatorConstants.FEED_SIMULATION_TYPE + " is null. Required '"
                            + EventSimulatorConstants.RANDOM_DATA_SIMULATION
                            + "' or '" + EventSimulatorConstants.FILE_FEED_SIMULATION + "' or '" +
                            EventSimulatorConstants.DATABASE_FEED_SIMULATION + "'");
                }
            }
            feedSimulationDto.setStreamConfigurationList(feedSimulationStreamConfigurationList);
            return feedSimulationDto;
    }

}
