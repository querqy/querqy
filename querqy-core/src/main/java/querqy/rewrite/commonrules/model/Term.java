package querqy.rewrite.commonrules.model;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.LowerCaseCharSequence;
import querqy.SimpleComparableCharSequence;

public class Term implements ComparableCharSequence {

   public final String FIELD_CHAR = ":";
   protected final char[] value;
   protected final int start;
   protected final int length;
   protected final List<String> fieldNames;
   protected final LinkedList<PlaceHolder> placeHolders;

   public Term(char[] value, int start, int length, List<String> fieldNames) {
       if (start + length > value.length) {
           throw new ArrayIndexOutOfBoundsException("start + length > value.length");
       }
       this.value = value;
       this.start = start;
       this.length = length;
       this.fieldNames = (fieldNames != null && fieldNames.isEmpty()) ? null : fieldNames;
       this.placeHolders = parsePlaceHolders();
   }
   
   private enum ParseState {None, Started, InRef}
   
   public int getMaxPlaceHolderRef() {
       return placeHolders == null ? -1 : placeHolders.getFirst().ref;
   }
   
   public boolean hasPlaceHolder() {
       return placeHolders != null && !placeHolders.isEmpty();
   }
   
   public ComparableCharSequence fillPlaceholders(TermMatches termMatches) {
       if (placeHolders == null || placeHolders.isEmpty()) {
           return this;
       }
       List<ComparableCharSequence> parts = new LinkedList<>();
       int pos = 0;
       for (PlaceHolder placeHolder: placeHolders) {
           if (placeHolder.start > pos) {
               parts.add(subSequence(pos, placeHolder.start));
           }
           parts.add(termMatches.getReplacement(placeHolder.ref));
           pos = placeHolder.start + placeHolder.length;
       }
       if (pos < length) {
           parts.add(subSequence(pos, start + length));
       }
       return new CompoundCharSequence(null, parts);
   }
   
   protected LinkedList<PlaceHolder> parsePlaceHolders() {
       
       LinkedList<PlaceHolder> placeHolders = new LinkedList<>();
       
       ParseState state = ParseState.None; 
       int begin = -1;
       int end = -1;
       
       for (int idx = start, last = start + length; idx < last; idx++) {
           char ch = value[idx];
           switch (state) {
           case None: 
               if (ch == '$') {
                   state = ParseState.Started;
               }
               break;
           case Started:
               if (Character.isDigit(ch)) {
                   begin = idx - 1;
                   end = idx;
                   state = ParseState.InRef;
               } else if (ch != '$') {
                   state = ParseState.None;
                   begin = -1;
               }
               break;
           case InRef:
               if (Character.isDigit(ch)) {
                   end = idx;
               } else {
                   int ref = Integer.parseInt(new String(value, begin + 1, end - begin));
                   PlaceHolder placeHolder = new PlaceHolder(begin, end - begin + 1, ref);
                   
                   if (placeHolders.isEmpty() || placeHolders.getFirst().ref < ref) {
                       placeHolders.addFirst(placeHolder);
                   } else {
                       placeHolders.add(placeHolder);
                   }
                   state = ch == '$' ? ParseState.Started : ParseState.None;
               }
               break;
           }
       }
       if (state == ParseState.InRef) {
           int ref = Integer.parseInt(new String(value, begin + 1, end - begin));
           PlaceHolder placeHolder = new PlaceHolder(begin, end - begin + 1, ref);
           
           if (placeHolders.isEmpty() || placeHolders.getFirst().ref < ref) {
               placeHolders.addFirst(placeHolder);
           } else {
               placeHolders.add(placeHolder);
           }
       }
       
       return placeHolders.isEmpty() ? null : placeHolders;
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

   public Term findFirstMatch(Collection<? extends Term> haystack) {

      for (Term h : haystack) {
         if (compareTo(h) == 0) {
            if (fieldNames == h.fieldNames) {
               return h;
            } else {
               if (h.fieldNames != null && fieldNames != null) {
                  for (String name : fieldNames) {
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
   public ComparableCharSequence subSequence(int start, int end) {
      if (end > length) {
         throw new ArrayIndexOutOfBoundsException(end);
      }
      if (start < 0) {
         throw new ArrayIndexOutOfBoundsException(start);
      }

      return new SimpleComparableCharSequence(value, this.start + start, end - start);
   }

   public List<ComparableCharSequence> getCharSequences(boolean lowerCaseValue) {
       
      SimpleComparableCharSequence seq = new SimpleComparableCharSequence(value, start, length);
      
      ComparableCharSequence valueSequence = lowerCaseValue ? new LowerCaseCharSequence(seq) : seq;
      
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

    public List<String> getFieldNames() {
        return fieldNames;
    }

    public LinkedList<PlaceHolder> getPlaceHolders() {
        return placeHolders;
    }
   
   

}