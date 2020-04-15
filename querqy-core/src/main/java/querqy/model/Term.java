/**
 * 
 */
package querqy.model;

import querqy.CharSequenceUtil;
import querqy.ComparableCharSequence;
import querqy.ComparableCharSequenceWrapper;
import querqy.CompoundCharSequence;
import querqy.LowerCaseCharSequence;
import querqy.SimpleComparableCharSequence;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class Term extends AbstractNode<DisjunctionMaxQuery> implements DisjunctionMaxClause, CharSequence, InputSequenceElement {

   protected final String field;
   protected final ComparableCharSequence value;

   public Term(final DisjunctionMaxQuery parentQuery, final String field, final CharSequence value, final boolean generated) {
      super(parentQuery, generated);
      this.field = field;
      this.value = ComparableCharSequence.class.isAssignableFrom(value.getClass()) 
              ? (ComparableCharSequence) value
              : new ComparableCharSequenceWrapper(value);
   }

   public Term(final DisjunctionMaxQuery parentQuery, final String field, final CharSequence value) {
      this(parentQuery, field, value, false);
   }

   public Term(final DisjunctionMaxQuery parentQuery, final CharSequence value) {
      this(parentQuery, null, value);
   }

   public Term(final DisjunctionMaxQuery parentQuery, final CharSequence value, final boolean generated) {
      this(parentQuery, null, value, generated);
   }

   public Term(final DisjunctionMaxQuery parentQuery, final String field, final char[] value,
               final int start, final int length, final boolean generated) {
      this(parentQuery, field, new SimpleComparableCharSequence(value, start, length), generated);
   }

   @Override
   public Term clone(final DisjunctionMaxQuery newParent) {
      return clone(newParent, isGenerated());
   }
   
   public Term clone(final DisjunctionMaxQuery newParent, final boolean isGenerated) {
       return new Term(newParent, field, value, isGenerated);
   }

   @Override
   public <T> T accept(final NodeVisitor<T> visitor) {
      return visitor.visit(this);
   }

   public String getField() {
      return field;
   }

   @Override
   public char charAt(final int index) {
      return value.charAt(index);
   }

   public ComparableCharSequence getValue() {
      return value;
   }

   @Override
   public String toString() {
      return ((field == null) ? "*" : field) + ":" + getValue();
   }

   @Override
   public int length() {
      return value.length();
   }

   @Override
   public ComparableCharSequence subSequence(final int start, final int end) {
      return value.subSequence(start, end);
   }

   public ComparableCharSequence toCharSequenceWithField(final boolean lowerCaseValue) {
       ComparableCharSequence valueToUse = lowerCaseValue ? new LowerCaseCharSequence(this) : value;
       return (field == null) ? valueToUse : new CompoundCharSequence(":", field, valueToUse);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((field == null) ? 0 : field.hashCode());
      result = prime * result + ((value == null) ? 0 : CharSequenceUtil.hashCode(value));
      return result;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      Term other = (Term) obj;
      if (field == null) {
         if (other.field != null) {
            return false;
         }
      } else if (!field.equals(other.field)) {
         return false;
      }
      if (value == null) {
         if (other.value != null) {
            return false;
         }
      } else if (! CharSequenceUtil.equals(this, other.value)) {
         return false;
      }
      return true;
   }

}
