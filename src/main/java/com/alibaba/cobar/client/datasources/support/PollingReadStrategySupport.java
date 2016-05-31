package com.alibaba.cobar.client.datasources.support;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.springframework.util.CollectionUtils;

import com.alibaba.cobar.client.datasources.IPartitionReadStrategy;
import com.alibaba.cobar.client.datasources.PartitionDataSource;

public class PollingReadStrategySupport implements IPartitionReadStrategy{
	
	private ConcurrentHashMap<String,AtomicLong> hash= new ConcurrentHashMap<String,AtomicLong>();

	@Override
	public DataSource getReadDataSource(PartitionDataSource ds) {
		return getDataSourceByPolling(ds,0);
	}
	
	protected DataSource getDataSourceByPolling(PartitionDataSource ds,int w){
		List<DataSource> readDataSources= ds.getReadDataSources();
		if(CollectionUtils.isEmpty(readDataSources)){
			return ds.getWriteDataSource();
		}
		AtomicLong next= hash.get(ds.getName());
		if(next== null){
			hash.putIfAbsent(ds.getName(), new AtomicLong(0));
			if((next= hash.get(ds.getName()))==null){
				return getDataSourceByPolling(ds,w);
			}
		}
    	int total= readDataSources.size()+ w,
        	idx= (int)(next.getAndIncrement() % total);
        return idx< readDataSources.size()? readDataSources.get(idx): ds.getWriteDataSource();
    }

}