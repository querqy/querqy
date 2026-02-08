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
        lookup.put("abc", "ABC");
        lookup.put("a(c{1,8}){3}d", "QQ");
        System.out.println(lookup.get("abc"));
        System.out.println(lookup.get("acccd"));
    }
}
