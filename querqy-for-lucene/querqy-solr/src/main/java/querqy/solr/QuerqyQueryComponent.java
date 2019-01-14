/**
 * 
 */
package querqy.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Query;
import org.apache.solr.handler.component.QueryComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.search.QParser;

import querqy.rewrite.ContextAwareQueryRewriter;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.model.DecorateInstruction;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class QuerqyQueryComponent extends QueryComponent {

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void prepare(ResponseBuilder rb) throws IOException {
        
        super.prepare(rb);
        
        QParser parser = rb.getQparser();
        
        if (parser instanceof QuerqyDismaxQParser) {
        
            List<Query> filterQueries = ((QuerqyDismaxQParser) parser).getFilterQueries();
            if ((filterQueries != null) && !filterQueries.isEmpty()) {
                List<Query> filters = rb.getFilters();
                if (filters == null) {
                    rb.setFilters(filterQueries);
                } else {
                    filters.addAll(filterQueries);
                }
            }
            rb.addDebugInfo("Querqy Criterion", ((QuerqyDismaxQParser) parser).getQuerqyCriterion());
            rb.addDebugInfo("Querqy Applied Rules",
                    ((QuerqyDismaxQParser) parser).getQuerqyAppliedRules());


        }
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#process(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void process(ResponseBuilder rb) throws IOException {
        
        super.process(rb);
        
        QParser parser = rb.getQparser();
        
        if (parser instanceof QuerqyDismaxQParser) {
            
            Map<String, Object> context = ((QuerqyDismaxQParser) parser).getContext();
            if (context != null) {

                @SuppressWarnings("unchecked")
                Set<Object> decorations = (Set<Object>) context.get(DecorateInstruction.CONTEXT_KEY);
                if (decorations != null) {
                    rb.rsp.add("querqy_decorations", decorations);
                }
                
            }
        }

    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#getDescription()
     */
    @Override
    public String getDescription() {
        return "Querqy search component";
    }

}
