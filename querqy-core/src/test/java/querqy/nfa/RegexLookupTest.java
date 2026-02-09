package querqy.nfa;

import org.junit.Test;
import querqy.regex.RegexLookup;
import querqy.trie.State;
import querqy.trie.TrieMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class RegexLookupTest {

    @Test
    public void testOne() {
        RegexLookup lookup = new RegexLookup();
        //lookup.put("abc", "ABC");
        //lookup.put("a(c{1,8}){3}d", "QQ");
        //lookup.put("a((c){1,2})", "QQ");
        lookup.put("a((\\d){2})", "QQ");
        System.out.println(lookup.get("a12"));
       // System.out.println(lookup.get("acccd"));
    }
}
