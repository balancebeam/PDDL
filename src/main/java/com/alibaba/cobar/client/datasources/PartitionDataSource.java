/**
 * Copyright 1999-2011 Alibaba Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package com.alibaba.cobar.client.datasources;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.alibaba.cobar.client.exception.ShardingDataSourceException;
import com.alibaba.cobar.client.exception.ShardingException;

/**
 * {@link PartitionDataSource} describe a data base deployment structure
 * with 2 databases as HA group.<br>
 * it looks like:<br>
 * 
 * <pre>
 *                  Client
 *                    /\
 *                  /    \
 *         Active DB <-> Standby DB
 * </pre> {@link #targetDataSource} should be the reference to the current active
 * database, while {@link #standbyDataSource} should be the standby database.<br>
 * for both {@link #targetDataSource} and {@link #standbyDataSource}, each one
 * should have a sibling data source that connect to the same target database.<br>
 * as to the reason why do so, that's :
 * <ol>
 * <li>these sibling DataSources will be used when do
 * database-status-detecting.(if we fetch connection from target data source,
 * when it's full, the deteting behavior can't be performed.)</li>
 * <li>if the {@link #targetDataSource} and {@link #standbyDataSource} are
 * DataSource implementations configured in local application container, we can
 * fetch necessary information via reflection to create connection to target
 * database independently, but if they are fetched from JNDI, we can't, so
 * explicitly declaring sibling data sources is necessary in this situation.</li>
 * </ol>
 * 
 * @author fujohnwang
 * @since 1.0
 */
public class PartitionDataSource{
    /**
     * the name of to-be-exposed DataSource.
     */
    private String name;
    /**
     * active data source
     */
    private DataSource writeDataSource;
    /**
     * read dataSource list
     */
    private List<DataSource> readDataSources;
    /**
     * detecting data source for active data source
     */
    private DataSource targetDetectorDataSource;
    /**
     * standby data source
     */
    private DataSource standbyDataSource;
    /**
     * detecting dataSource for standby data source
     */
    private DataSource standbyDetectorDataSource;

    /**
     * we will initialize proper thread pools which stand in front of data
     * sources as per connection pool size. <br>
     * usually, they will have same number of objects.<br>
     * you have to set a proper size for this attribute as per your data source
     * attributes. In case you forget it, we set a default value with
     * "number of CPU" * 5.
     */
    private int poolSize = Runtime.getRuntime().availableProcessors() * 2;
    
    /**
     * only-write: use write dataSource only to execute DQL SQL ,default read strategy.
     * polling: use read dataSource only to execute DQL SQL with polling read strategy.
     * polling-w: use read and write dataSource to execute DQL SQL with polling read strategy.
     * power: use read dataSource only to execute DQL SQL with power read strategy.
     * power-w: use read and write dataSource to execute DQL SQL with power read strategy.
     * weight: use read dataSource only to execute DQL SQL with weight read strategy.
     * weight-w: use read and write dataSource to execute DQL SQL with weight read strategy.
     */
    private String readStrategy= "only-write";
    
    private PartitionReadStrategyRepository readStrategyRepository;
    
    private boolean defaultDataSource= false;
    
	public void setReadStrategy(String readStrategy){
		this.readStrategy= readStrategy;
	}
	
	public void setDefaultDataSource(boolean defaultDataSource){
		this.defaultDataSource= defaultDataSource;
	}
	
	public boolean isDefaultDataSource(){
		return defaultDataSource;
	}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataSource getWriteDataSource() {
        return writeDataSource;
    }
    
    void setReadStrategyRepository(PartitionReadStrategyRepository readStrategyRepository){
    	this.readStrategyRepository= readStrategyRepository;
    }

    public void setWriteDataSource(DataSource writeDataSource) {
    	Assert.notNull(writeDataSource);
    	if(this.writeDataSource!= null){
    		throw new ShardingDataSourceException("Partition ["+name+"] write dataSource setting is duplicated");
    	}
    	this.writeDataSource = writeDataSource instanceof CobarDataSourceProxy? writeDataSource: new CobarDataSourceProxy(writeDataSource);
    	((CobarDataSourceProxy)this.writeDataSource).setPartitionDataSource(this);
    }
    
    public DataSource getReadDataSource(){
    	if(readStrategyRepository== null){
    		return writeDataSource;
    	}
    	IPartitionReadStrategy readStrategySupport= readStrategyRepository.getReadStrategy(readStrategy);
    	if(readStrategySupport== null){
    		throw new ShardingException("not support ["+readStrategy+"] read strategy ");
    	}
    	return readStrategySupport.getReadDataSource(this);
    }
    
    public void setReadDataSources(List<DataSource> readDataSources){
    	Assert.notEmpty(readDataSources);
    	if(!CollectionUtils.isEmpty(this.readDataSources)){
    		throw new ShardingDataSourceException("Partition ["+name+"] read dataSources setting is duplicated");
    	}
    	this.readDataSources= new ArrayList<DataSource>();
    	for(DataSource dataSource: readDataSources){
    		DataSource ds = dataSource instanceof CobarDataSourceProxy? dataSource: new CobarDataSourceProxy(dataSource);
        	((CobarDataSourceProxy)ds).setPartitionDataSource(this);
    		this.readDataSources.add(ds);
    	}
    }
    
    public List<DataSource> getReadDataSources(){
    	return readDataSources;
    }

    public DataSource getTargetDetectorDataSource() {
        return targetDetectorDataSource;
    }

    public void setTargetDetectorDataSource(DataSource targetDetectorDataSource) {
        this.targetDetectorDataSource = targetDetectorDataSource;
    }
    
    public DataSource getStandbyDataSource() {
        return standbyDataSource;
    }

    public void setStandbyDataSource(DataSource standbyDataSource) {
        this.standbyDataSource = standbyDataSource;
    }

    public DataSource getStandbyDetectorDataSource() {
        return standbyDetectorDataSource;
    }

    public void setStandbyDetectorDataSource(DataSource standbyDetectorDataSource) {
        this.standbyDetectorDataSource = standbyDetectorDataSource;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getPoolSize() {
        return poolSize;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PartitionDataSource other = (PartitionDataSource) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PartitionDataSource [name=" + name + ", poolSize=" + poolSize
                + ", standbyDataSource=" + standbyDataSource + ", standbyDetectorDataSource="
                + standbyDetectorDataSource + ", writeDataSource=" + writeDataSource
                + ", readDataSources=" + readDataSources + "]";
    }
    
}
