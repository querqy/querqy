/**
 * 
 */
package querqy.solr;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;

import querqy.rewrite.commonrules.model.DecorateInstruction;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class QuerqySearchComponent extends SearchComponent {

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void prepare(ResponseBuilder rb) throws IOException {
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#process(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void process(ResponseBuilder rb) throws IOException {
        
        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) rb.req.getContext().get(QuerqyDismaxQParser.CONTEXT_ATTRIBUTE_NAME);
        
        if (context != null) {
        
            @SuppressWarnings("unchecked")
            Set<Object> decorations = (Set<Object>) context.get(DecorateInstruction.CONTEXT_KEY);
            if (decorations != null) {
                rb.rsp.add("querqy_decorations", decorations);
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

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#getSource()
     */
    @Override
    public String getSource() {
        return null;
    }

}
