/*
 Copyright 2012 Adam Bien, adam-bien.com

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.lightfish.business.monitoring.control;

import java.util.ArrayList;
import java.util.Date;
import org.lightfish.business.monitoring.entity.Snapshot;

import javax.inject.Inject;
import java.util.List;
import javax.enterprise.inject.Instance;
import java.util.logging.Logger;
import org.lightfish.business.monitoring.control.collectors.DataCollector;
import org.lightfish.business.monitoring.control.collectors.DataPoint;
import org.lightfish.business.monitoring.control.collectors.DataPointToSnapshotMapper;
import org.lightfish.business.monitoring.control.collectors.ParallelDataCollectionActionBehaviour;
import org.lightfish.business.monitoring.control.collectors.ParallelDataCollectionExecutor;
import org.lightfish.business.monitoring.control.collectors.SnapshotDataCollector;

/**
 * @author Adam Bien, blog.adam-bien.com / Rob Veldpaus
 */
public class SnapshotProvider {

    private static final Logger LOG = Logger.getLogger(SnapshotProvider.class.getName());
    @Inject
    Instance<Boolean> parallelDataCollection;
    @Inject
    @SnapshotDataCollector
    Instance<DataCollector> dataCollectors;
    @Inject
    DataPointToSnapshotMapper mapper;
    @Inject
    Instance<Integer> dataCollectionRetries;
    @Inject
    ParallelDataCollectionExecutor parallelExecutor;

    public Snapshot fetchSnapshot(String instanceName) throws Exception {
        Snapshot snapshot = null;
        Date start = new Date();

        if (parallelDataCollection.get()) {
            snapshot = parallelDataCollection(instanceName);
        } else {
            snapshot = serialDataCollection(instanceName);
        }

        long elapsed = new Date().getTime() - start.getTime();
        LOG.fine("Data collection took " + elapsed);

        return snapshot;

    }

    private Snapshot serialDataCollection(String instanceName) throws Exception {
        Snapshot snapshot = new Snapshot.Builder().build();
        DataCollectionBehaviour dataCollectionBehaviour = new DataCollectionBehaviour(mapper, snapshot);
        for (DataCollector collector : retrieveDataCollectorList(instanceName)) {
            DataPoint dataPoint = serialDataCollect(collector, 0);
            dataCollectionBehaviour.perform(dataPoint);
        }
        return snapshot;
    }

    private DataPoint serialDataCollect(DataCollector collector, int attempt) throws Exception {
        try {
            return collector.collect();
        } catch (Exception ex) {
            if (attempt < dataCollectionRetries.get()) {
                return serialDataCollect(collector, ++attempt);
            } else {
                throw ex;
            }
        }
    }

    private Snapshot parallelDataCollection(String instanceName) throws Exception {
        Snapshot snapshot = new Snapshot.Builder().build();
        parallelExecutor.execute(new DataCollectionBehaviour(mapper, snapshot), retrieveDataCollectorList(instanceName));

        return snapshot;
    }

    private List<DataCollector> retrieveDataCollectorList(String instanceName) {
        List<DataCollector> dataCollectorList = new ArrayList<>();
        for (DataCollector collector : dataCollectors) {
            collector.setServerInstance(instanceName);
            dataCollectorList.add(collector);

        }
        return dataCollectorList;
    }

    private class DataCollectionBehaviour implements ParallelDataCollectionActionBehaviour {

        private DataPointToSnapshotMapper mapper;
        private Snapshot snapshot;

        public DataCollectionBehaviour(DataPointToSnapshotMapper mapper, Snapshot snapshot) {
            this.mapper = mapper;
            this.snapshot = snapshot;
        }

        @Override
        public void perform(DataPoint dataPoint) throws Exception {
            if (dataPoint != null) {
                mapper.mapDataPointToSnapshot(dataPoint, snapshot);
            }
        }
    }
}
