package org.neo4j.training.backend;

import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIFacade;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.util.ResultConverter;

import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 30.05.12
 */
public class Neo4jGraphStorage implements GraphStorage {

    private final RestAPI restAPI;
    private final RestCypherQueryEngine cypher;
    private final RestIndex<Node> index;

    public Neo4jGraphStorage(String uri) {
        this(new RestAPIFacade(uri));
    }

    public Neo4jGraphStorage(RestAPI restAPI) {
        this.restAPI = restAPI;
        cypher = new RestCypherQueryEngine(restAPI);
        index = restAPI.index().forNodes("graphs");
    }

    @Override
    public GraphInfo find(String id) {
        if (id==null) return null;
        return cypher.query("MATCH (n:Graph {id :{id}}) set n.count = coalesce(n.count,0)+1, n.time={time} return n", map("id", id,"time",System.currentTimeMillis())).to(GraphInfo.class, new ResultConverter<Map<String, Object>, GraphInfo>() {
            @Override
            public GraphInfo convert(Map<String, Object> row, Class<GraphInfo> type) {
                if (row==null || row.get("n")==null) return null;
                return new GraphInfo((Node)row.get("n"));
            }
        }).singleOrNull();
    }

    @Override
    public void update(GraphInfo info) {
        cypher.query("MATCH (n:Graph {id :{id}}) where not(n is null) set n.query=coalesce({query},'none'), n.init=coalesce({init},'none'), n.message=coalesce({message},'none'),n.history=coalesce({history},'')", info.toMap());
    }

    @Override
    public GraphInfo create(GraphInfo info) {
        if (isEmpty(info)) info = info.withId(Util.randomId());
        Map result = cypher.query("MERGE (n:Graph {id :{id}}) set n = {props}", map("id", info.getId(),"props",info.toMap())).to(Map.class).singleOrNull();
        return new GraphInfo(result);
    }

    private boolean isEmpty(GraphInfo info) {
        return info.getId()==null || info.getId().trim().isEmpty();
    }

    @Override
    public void delete(String id) {
        cypher.query("MATCH (n:Graph {id :{id}}) delete n", map("id", id));
    }
}
