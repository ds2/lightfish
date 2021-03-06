package org.lightfish.business.monitoring.control.collectors.paranormal;

import org.lightfish.business.monitoring.control.collectors.jvm.*;
import org.lightfish.business.monitoring.control.collectors.AbstractRestDataCollector;
import org.lightfish.business.monitoring.control.collectors.DataPoint;
import org.lightfish.business.monitoring.control.collectors.SnapshotDataCollector;

/**
 *
 * @author Rob Veldpaus
 */
@SnapshotDataCollector
public class ErrorCountCollector extends AbstractRestDataCollector<Integer> {
    
    public static final String ERROR_COUNT_URI = "http-service/server/request/errorcount";

    @Override
    public DataPoint<Integer> collect() throws Exception{
        return new DataPoint<>("errorCount",getInt(ERROR_COUNT_URI, "errorcount"));
    }
    
}
