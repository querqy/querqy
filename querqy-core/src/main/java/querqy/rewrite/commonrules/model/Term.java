package querqy.rewrite.commonrules.model;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.SimpleComparableCharSequence;

public class Term implements ComparableCharSequence {

   public final String FIELD_CHAR = ":";
   protected final char[] value;
   protected final int start;
   protected final int length;
   protected final List<String> fieldNames;

   public Term(char[] value, int start, int length, List<String> fieldNames) {
      this.value = value;
      this.start = start;
      this.length = length;
      this.fieldNames = (fieldNames != null && fieldNames.isEmpty()) ? null : fieldNames;
   }

   @Override
   public char charAt(int idx) {
      if (idx >= length) {
         throw new ArrayIndexOutOfBoundsException(idx);
      }
      return value[start + idx];
   }

   @Override
   public int compareTo(CharSequence other) {

      for (int i = 0, pos = start, len = Math.min(length, other.length()); i < len; i++) {
         char ch1 = value[pos++];
         char ch2 = other.charAt(i);
         if (ch1 != ch2) {
            return ch1 - ch2;
         }
      }

      return length - other.length();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;

      result = prime * result
            + ((fieldNames == null) ? 0 : fieldNames.hashCode());
      result = prime * result + length;

      for (int i = 0; i < length; i++) {
         result = prime * result + value[start + i];
      }

      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;

      if (!Term.class.isAssignableFrom(obj.getClass()))
         return false;

      Term other = (Term) obj;

      if (fieldNames == null) {
         if (other.fieldNames != null)
            return false;
      } else if (!fieldNames.equals(other.fieldNames))
         return false;
      if (length != other.length)
         return false;

      for (int i = 0; i < length; i++) {
         if (value[start + i] != other.value[other.start + i]) {
            return false;
         }
      }

      return true;
   }

   @Override
   public String toString() {
      return "Term [fieldNames="
            + fieldNames + ", value=" + new String(value, start, length) + "]";
   }

   public static Term findFirstMatch(Term needle, Collection<? extends Term> haystack) {

      for (Term h : haystack) {
         if (needle.compareTo(h) == 0) {
            if (needle.fieldNames == h.fieldNames) {
               return h;
            } else {
               if (h.fieldNames != null && needle.fieldNames != null) {
                  for (String name : needle.fieldNames) {
                     if (h.fieldNames.contains(name)) {
                        return h;
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   @Override
   public int length() {
      return length;
   }

   @Override
   public CharSequence subSequence(int start, int end) {
      if (end > length) {
         throw new ArrayIndexOutOfBoundsException(end);
      }
      if (start < 0) {
         throw new ArrayIndexOutOfBoundsException(start);
      }

      return new SimpleComparableCharSequence(value, this.start + start, end - start);
   }

   public List<ComparableCharSequence> getCharSequences() {
      SimpleComparableCharSequence valueSequence = new SimpleComparableCharSequence(value, start, length);
      List<ComparableCharSequence> seqs = new LinkedList<>();
      if (fieldNames == null) {
         seqs.add(valueSequence);
      } else {
         for (String name : fieldNames) {
            seqs.add(new CompoundCharSequence(FIELD_CHAR, name, valueSequence));
         }
      }

      return seqs;
   }

}