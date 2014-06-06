package querqy;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import querqy.model.BooleanClause;
import querqy.model.BooleanQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.SubQuery;
import querqy.model.Term;
import querqy.model.SubQuery.Occur;

public class AbstractQueryTest {

	public TermMatcher term(String field, String value) {
		return new TermMatcher(field, value);
	}

	public TermMatcher term(String value) {
		return new TermMatcher(null, value);
	}

	@SafeVarargs
	public final DMQMatcher dmq(TypeSafeMatcher<? extends DisjunctionMaxClause>... clauses) {
		return new DMQMatcher(clauses);
	}
	@SafeVarargs
	public final DMQMatcher dmq(OccurMatcher occur, TypeSafeMatcher<? extends DisjunctionMaxClause>... clauses) {
	    return new DMQMatcher(occur, clauses);
	}

	@SafeVarargs
	public final BQMatcher bq(TypeSafeMatcher<? extends BooleanClause>... clauses) {
		return new BQMatcher(clauses);
	}
	@SafeVarargs
	public final BQMatcher bq(OccurMatcher occur, TypeSafeMatcher<? extends BooleanClause>... clauses) {
	    return new BQMatcher(occur, clauses);
	}

	public OccurMatcher must() {return new OccurMatcher(Occur.MUST); }
	
	public OccurMatcher mustNot() {return new OccurMatcher(Occur.MUST_NOT); }
	
	public OccurMatcher should() {return new OccurMatcher(Occur.SHOULD); }
	
	class SubQueryMatcher<T extends SubQuery<? extends Clause>> extends TypeSafeMatcher<T> {
		
		final TypeSafeMatcher<? extends Clause>[] clauses;
		final OccurMatcher occur;
		
		public SubQueryMatcher(OccurMatcher occur, TypeSafeMatcher<? extends Clause>[] clauses) {
			this.clauses = clauses;
			this.occur = occur;
		}

		@Override
		public void describeTo(Description description) {
		    occur.describeTo(description);
			description.appendList("clauses:[", ",\n", "]", Arrays.asList(clauses));
			
		}

		@Override
		protected boolean matchesSafely(T item) {
		    if (!occur.matches(item.occur)) {
		        return false;
		    }
			List<? extends Clause> itemClauses = item.getClauses();
			if (itemClauses == null || itemClauses.size() != clauses.length) {
				return false;
			}
			for (int i = 0; i < clauses.length; i++) {
				if (!clauses[i].matches(itemClauses.get(i))) {
					return false;
				}
			}
			return true;
		}
		
	}
	
	class DMQMatcher extends SubQueryMatcher<DisjunctionMaxQuery> {
	    public DMQMatcher(OccurMatcher occur, TypeSafeMatcher<? extends DisjunctionMaxClause>[] clauses) {
            super(occur, clauses);
        }
		public DMQMatcher(TypeSafeMatcher<? extends DisjunctionMaxClause>[] clauses) {
			super(should(), clauses);
		}
		
		@Override
		public void describeTo(Description description) {
			description.appendText("DMQ:\n ");
			super.describeTo(description);
		}
	}
	
	
	class BQMatcher extends SubQueryMatcher<BooleanQuery> {
		
		public BQMatcher(OccurMatcher occur, TypeSafeMatcher<? extends BooleanClause>[] clauses) {
			super(occur, clauses);
		}
		public BQMatcher(TypeSafeMatcher<? extends BooleanClause>[] clauses) {
		    super(should(), clauses);
		}
		

		@Override
		public void describeTo(Description description) {
			description.appendText("BQ: ");
			description.appendText("\n (");
			super.describeTo(description);
			description.appendText(")\n");
		}

		@Override
		protected boolean matchesSafely(BooleanQuery bq ) {
			return super.matchesSafely(bq);
		}
		
	}
	
	
	class OccurMatcher extends TypeSafeMatcher<Occur> {
	    
	    final Occur expected;
	    
	    public OccurMatcher(Occur expected) {
	        this.expected = expected;
	    }

        @Override
        public void describeTo(Description description) {
            description.appendText("occur: " + expected.name());
        }

        @Override
        protected boolean matchesSafely(Occur occur) {
            return occur == expected;
        }
	    
	}
	
	
	
	class TermMatcher extends TypeSafeMatcher<Term> {
		
		final String expectedField;
		final String expectedValue;
		
		public TermMatcher(String field, String value) {
			this.expectedField = field;
			this.expectedValue = value;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("term: " + String.valueOf(expectedField) + ":" + String.valueOf(expectedValue)); 
		}

		@Override
		protected boolean matchesSafely(Term item) {
			return item != null
					&& ((item.getField() == expectedField) 
							|| (item.getField() != null && item.getField().equals(expectedField)))
					&& ((item.getValue() == expectedValue) 
							|| (item.getValue() != null && item.getValue().equals(expectedValue)));
		}
		
	}

}