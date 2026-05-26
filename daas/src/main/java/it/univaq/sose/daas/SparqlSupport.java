package it.univaq.sose.daas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

final class SparqlSupport {

    private static final String SPARQL_ENDPOINT = "http://triplestore:3030/dataset/query";

    private SparqlSupport() {
    }

    static List<Map<String, String>> executeSparql(String queryString, String[] vars, String prefixes) {
        List<Map<String, String>> list = new ArrayList<>();
        String query = queryString.startsWith("PREFIX ") ? queryString : prefixes + queryString;
        try (QueryExecution qexec = QueryExecution.service(SPARQL_ENDPOINT).query(query).build()) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Map<String, String> map = new HashMap<>();
                for (String var : vars) {
                    if (soln.contains(var)) {
                        RDFNode node = soln.get(var);
                        map.put(var, node.isLiteral() ? node.asLiteral().getString() : node.toString());
                    }
                }
                list.add(map);
            }
        }
        return list;
    }

    static boolean isValidIdentifier(String value) {
        return value != null && !value.isBlank() && value.matches("[A-Za-z0-9_-]+");
    }
}