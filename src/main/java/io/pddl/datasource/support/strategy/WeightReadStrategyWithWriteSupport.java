package io.pddl.datasource.support.strategy;

import javax.sql.DataSource;

import io.pddl.datasource.PartitionDataSource;
import io.pddl.datasource.support.DefaultDataSourceProxy;

public class WeightReadStrategyWithWriteSupport extends WeightReadStrategySupport{
	
	@Override
	public DataSource getReadDataSource(PartitionDataSource ds) {
		return getDataSourceByWeight(ds,((DefaultDataSourceProxy)ds.getWriteDataSource()).getWeight());
	}
	
	@Override
	public String getStrategyName(){
		return "weight-w";
	}
}
