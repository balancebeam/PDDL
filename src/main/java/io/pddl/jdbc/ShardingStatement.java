package io.pddl.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import io.pddl.executor.ExecuteStatementCallback;
import io.pddl.executor.support.ExecuteStatementWrapper;
import io.pddl.executor.ExecuteStatementProcessor;
import io.pddl.jdbc.adapter.AbstractStatementAdapter;
import io.pddl.merger.MergeUtils;
import io.pddl.router.SQLRouter;
import io.pddl.router.support.SQLExecutionUnit;

public class ShardingStatement extends AbstractStatementAdapter {
    
    protected ShardingConnection shardingConnection;
    
    private int resultSetType;
    
    private int resultSetConcurrency;
    
    private int resultSetHoldability;
    
    protected ExecuteStatementProcessor processor;
    
    protected SQLRouter sqlRouter;
    
    private Map<HashCode, Statement> cachedRoutedStatements = new HashMap<HashCode, Statement>();
    
    protected ResultSet currentResultSet;
    
    public ShardingStatement(final ShardingConnection shardingConnection) {
        this(shardingConnection, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }
    
    public ShardingStatement(final ShardingConnection shardingConnection, final int resultSetType, final int resultSetConcurrency) {
        this(shardingConnection, resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }
    
    public ShardingStatement(final ShardingConnection shardingConnection, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
        this.shardingConnection = shardingConnection;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;
        this.sqlRouter= shardingConnection.shardingDataSource.sqlRouter;
        this.processor= shardingConnection.shardingDataSource.processor;
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return shardingConnection;
    }
    
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        if (null != currentResultSet && !currentResultSet.isClosed()) {
            currentResultSet.close();
        }
        
        List<ResultSet> result= processor.execute(shardingConnection.getExecuteContext(),generateExecuteStatementWrappers(sql), new ExecuteStatementCallback<Statement,ResultSet>(){
			@Override
			public ResultSet execute(String shardingSql,Statement statement) throws SQLException {
				return statement.executeQuery(shardingSql);
			}
    	});
        return currentResultSet= MergeUtils.mergeResultSet(result);
    }
    
    @Override
    public int executeUpdate(final String sql) throws SQLException {
    	List<Integer> result= processor.execute(shardingConnection.getExecuteContext(),generateExecuteStatementWrappers(sql), new ExecuteStatementCallback<Statement,Integer>(){
			@Override
			public Integer execute(String shardingSql,Statement statement) throws SQLException {
				return statement.executeUpdate(shardingSql);
			}
    	});
    	return MergeUtils.mergeIntegerResult(result);
    }
    
    @Override
    public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
    	List<Integer> result= processor.execute(shardingConnection.getExecuteContext(),generateExecuteStatementWrappers(sql), new ExecuteStatementCallback<Statement,Integer>(){
			@Override
			public Integer execute(String shardingSql,Statement statement) throws SQLException {
				return statement.executeUpdate(shardingSql,autoGeneratedKeys);
			}
    	});
    	return MergeUtils.mergeIntegerResult(result);
    }
    
    @Override
    public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
    	List<Integer> result= processor.execute(shardingConnection.getExecuteContext(),generateExecuteStatementWrappers(sql), new ExecuteStatementCallback<Statement,Integer>(){
			@Override
			public Integer execute(String shardingSql,Statement statement) throws SQLException {
				return statement.executeUpdate(shardingSql,columnIndexes);
			}
    	});
    	return MergeUtils.mergeIntegerResult(result);
    }
    
    @Override
    public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
    	List<Integer> result= processor.execute(shardingConnection.getExecuteContext(),generateExecuteStatementWrappers(sql), new ExecuteStatementCallback<Statement,Integer>(){
			@Override
			public Integer execute(String shardingSql,Statement statement) throws SQLException {
				return statement.executeUpdate(shardingSql,columnNames);
			}
    	});
    	return MergeUtils.mergeIntegerResult(result);
    }
    
    @Override
    public boolean execute(final String sql) throws SQLException {
    	List<Boolean> result= processor.execute(shardingConnection.getExecuteContext(),generateExecuteStatementWrappers(sql), new ExecuteStatementCallback<Statement,Boolean>(){
			@Override
			public Boolean execute(String shardingSql,Statement statement) throws SQLException {
				return statement.execute(shardingSql);
			}
    	});
    	return result.get(0);
    }
    
    @Override
    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
    	List<Boolean> result= processor.execute(shardingConnection.getExecuteContext(),generateExecuteStatementWrappers(sql), new ExecuteStatementCallback<Statement,Boolean>(){
			@Override
			public Boolean execute(String shardingSql,Statement statement) throws SQLException {
				return statement.execute(shardingSql,autoGeneratedKeys);
			}
    	});
    	return result.get(0);
    }
    
    @Override
    public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
    	List<Boolean> result= processor.execute(shardingConnection.getExecuteContext(),generateExecuteStatementWrappers(sql), new ExecuteStatementCallback<Statement,Boolean>(){
			@Override
			public Boolean execute(String shardingSql,Statement statement) throws SQLException {
				return statement.execute(shardingSql,columnIndexes);
			}
    	});
    	return result.get(0);
    }
    
    @Override
    public boolean execute(final String sql, final String[] columnNames) throws SQLException {
    	List<Boolean> result= processor.execute(shardingConnection.getExecuteContext(),generateExecuteStatementWrappers(sql), new ExecuteStatementCallback<Statement,Boolean>(){
			@Override
			public Boolean execute(String shardingSql,Statement statement) throws SQLException {
				return statement.execute(shardingSql,columnNames);
			}
    	});
    	return result.get(0);
    }
    
    private List<ExecuteStatementWrapper<Statement>> generateExecuteStatementWrappers(final String sql) throws SQLException {
    	List<SQLExecutionUnit> executionUnits = sqlRouter.doRoute(shardingConnection.getExecuteContext(),sql, Collections.emptyList());
        List<ExecuteStatementWrapper<Statement>> result= new ArrayList<ExecuteStatementWrapper<Statement>>(executionUnits.size());
    	for (SQLExecutionUnit it : executionUnits) {
        	Statement statement= generateStatement(it.getShardingSql(),it.getDataSourceName());
        	result.add(new ExecuteStatementWrapper<Statement>(it,statement));
        }
        return result;
    }
    
    private Statement generateStatement(final String sql, final String dataSourceName) throws SQLException {
        HashCode hashCode =  Hashing.md5().newHasher().putString(sql, Charsets.UTF_8).putString(dataSourceName, Charsets.UTF_8).hash();
        if (cachedRoutedStatements.containsKey(hashCode)) {
            return cachedRoutedStatements.get(hashCode);
        }
        Connection connection = shardingConnection.getConnection(dataSourceName);
        Statement result;
        if (0 == resultSetHoldability) {
            result = connection.createStatement(resultSetType, resultSetConcurrency);
        } else {
            result = connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        cachedRoutedStatements.put(hashCode, result);
        return result;
    }
    
    @Override
    public ResultSet getResultSet() throws SQLException {
        if (null != currentResultSet) {
            return currentResultSet;
        }
        List<ResultSet> resultSets = new ArrayList<ResultSet>(getRoutedStatements().size());
        for (Statement each : getRoutedStatements()) {
            resultSets.add(each.getResultSet());
        }
        return currentResultSet = MergeUtils.mergeResultSet(resultSets);
    }
    
    @Override
    public Collection<? extends Statement> getRoutedStatements() throws SQLException {
        return cachedRoutedStatements.values();
    }
    
    @Override
    public void clearRoutedStatements() throws SQLException {
        cachedRoutedStatements.clear();
    }

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return resultSetConcurrency;
	}

	@Override
	public int getResultSetType() throws SQLException {
		return resultSetType;
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return resultSetHoldability;
	}
}
