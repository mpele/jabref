/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.groups.structure;

import java.util.regex.Pattern;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;
import net.sf.jabref.search.SearchRule;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;
import net.sf.jabref.util.QuotedStringTokenizer;
import net.sf.jabref.util.StringUtil;

/**
 * @author jzieren
 */
public class KeywordGroup extends AbstractGroup {

    public static final String ID = "KeywordGroup:";

    private final String searchField;
    private final String searchExpression;
    private final boolean caseSensitive;
    private final boolean regExp;
    private Pattern pattern = null;

    /**
     * Creates a KeywordGroup with the specified properties.
     */
    public KeywordGroup(String name, String searchField,
                        String searchExpression, boolean caseSensitive, boolean regExp,
                        GroupHierarchyType context) throws IllegalArgumentException {
        super(name, context);
        this.searchField = searchField;
        this.searchExpression = searchExpression;
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
        if (this.regExp) {
            compilePattern();
        }
    }

    private void compilePattern() throws IllegalArgumentException {
        pattern = caseSensitive ? Pattern.compile("\\b" + searchExpression + "\\b")
                : Pattern.compile("\\b" + searchExpression + "\\b", Pattern.CASE_INSENSITIVE);
    }

    /**
     * Parses s and recreates the KeywordGroup from it.
     *
     * @param s The String representation obtained from
     *          KeywordGroup.toString()
     */
    public static AbstractGroup fromString(String s, BibtexDatabase db,
                                           int version) throws Exception {
        if (!s.startsWith(KeywordGroup.ID)) {
            throw new Exception(
                    "Internal error: KeywordGroup cannot be created from \""
                            + s
                            + "\". "
                            + "Please report this on www.sf.net/projects/jabref");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(KeywordGroup.ID
                .length()), AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR);
        switch (version) {
            case 0: {
                String name = tok.nextToken();
                String field = tok.nextToken();
                String expression = tok.nextToken();
                // assume caseSensitive=false and regExp=true for old groups
                return new KeywordGroup(StringUtil.unquote(name, AbstractGroup.QUOTE_CHAR), StringUtil
                        .unquote(field, AbstractGroup.QUOTE_CHAR), StringUtil.unquote(expression,
                        AbstractGroup.QUOTE_CHAR), false, true, GroupHierarchyType.INDEPENDENT);
            }
            case 1:
            case 2: {
                String name = tok.nextToken();
                String field = tok.nextToken();
                String expression = tok.nextToken();
                boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
                boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
                return new KeywordGroup(StringUtil.unquote(name, AbstractGroup.QUOTE_CHAR), StringUtil
                        .unquote(field, AbstractGroup.QUOTE_CHAR), StringUtil.unquote(expression,
                        AbstractGroup.QUOTE_CHAR), caseSensitive, regExp,
                        GroupHierarchyType.INDEPENDENT);
            }
            case 3: {
                String name = tok.nextToken();
                int context = Integer.parseInt(tok.nextToken());
                String field = tok.nextToken();
                String expression = tok.nextToken();
                boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
                boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
                return new KeywordGroup(StringUtil.unquote(name, AbstractGroup.QUOTE_CHAR), StringUtil
                        .unquote(field, AbstractGroup.QUOTE_CHAR), StringUtil.unquote(expression,
                        AbstractGroup.QUOTE_CHAR), caseSensitive, regExp, GroupHierarchyType.getByNumber(context));
            }
            default:
                throw new UnsupportedVersionException("KeywordGroup", version);
        }
    }

    /**
     * @see AbstractGroup#getSearchRule()
     */
    @Override
    public SearchRule getSearchRule() {
        return new SearchRule() {
            @Override
            public int applyRule(String query, BibtexEntry bibtexEntry) {
                return contains(query, bibtexEntry) ? 1 : 0;
            }

            @Override
            public boolean validateSearchStrings(String query) {
                return true;
            }
        };
    }

    /**
     * Returns a String representation of this object that can be used to
     * reconstruct it.
     */
    @Override
    public String toString() {
        return KeywordGroup.ID + StringUtil.quote(name, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR) + AbstractGroup.SEPARATOR
                + context + AbstractGroup.SEPARATOR
                + StringUtil.quote(searchField, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR) + AbstractGroup.SEPARATOR
                + StringUtil.quote(searchExpression, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)
                + AbstractGroup.SEPARATOR + StringUtil.booleanToBinaryString(caseSensitive) + AbstractGroup.SEPARATOR
                + StringUtil.booleanToBinaryString(regExp) + AbstractGroup.SEPARATOR;
    }

    @Override
    public boolean supportsAdd() {
        return !regExp;
    }

    @Override
    public boolean supportsRemove() {
        return !regExp;
    }

    @Override
    public AbstractUndoableEdit add(BibtexEntry[] entries) {
        if (!supportsAdd()) {
            return null;
        }
        if ((entries != null) && (entries.length > 0)) {
            NamedCompound ce = new NamedCompound(
                    Globals.lang("add entries to group"));
            boolean modified = false;
            for (BibtexEntry entry : entries) {
                if (getSearchRule().applyRule(SearchRule.NULL_QUERY, entry) == 0) {
                    String oldContent = entry
                            .getField(searchField), pre = Globals.prefs.get(JabRefPreferences.GROUP_KEYWORD_SEPARATOR);
                    String newContent = (oldContent == null ? "" : oldContent
                            + pre)
                            + searchExpression;
                    entry.setField(searchField, newContent);

                    // Store undo information.
                    ce.addEdit(new UndoableFieldChange(entry,
                            searchField, oldContent, newContent));
                    modified = true;
                }
            }
            if (modified) {
                ce.end();
            }

            return modified ? ce : null;
        }

        return null;
    }

    @Override
    public AbstractUndoableEdit remove(BibtexEntry[] entries) {
        if (!supportsRemove()) {
            return null;
        }

        if ((entries != null) && (entries.length > 0)) {
            NamedCompound ce = new NamedCompound(Globals.lang("remove from group"));
            boolean modified = false;
            for (BibtexEntry entry : entries) {
                if (getSearchRule().applyRule(SearchRule.NULL_QUERY, entry) > 0) {
                    String oldContent = entry
                            .getField(searchField);
                    removeMatches(entry);
                    // Store undo information.
                    ce.addEdit(new UndoableFieldChange(entry,
                            searchField, oldContent, entry
                            .getField(searchField)
                    ));
                    modified = true;
                }
            }
            if (modified) {
                ce.end();
            }

            return modified ? ce : null;
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof KeywordGroup)) {
            return false;
        }
        KeywordGroup other = (KeywordGroup) o;
        return name.equals(other.name)
                && searchField.equals(other.searchField)
                && searchExpression.equals(other.searchExpression)
                && (caseSensitive == other.caseSensitive)
                && (regExp == other.regExp)
                && (getHierarchicalContext() == other.getHierarchicalContext());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.jabref.groups.structure.AbstractGroup#contains(java.util.Map,
     *      net.sf.jabref.BibtexEntry)
     */
    @Override
    public boolean contains(String query, BibtexEntry entry) {
        return contains(entry);
    }

    @Override
    public boolean contains(BibtexEntry entry) {
        String content = entry.getField(searchField);
        if (content == null) {
            return false;
        }
        if (regExp) {
            return pattern.matcher(content).find();
        }
        if (caseSensitive) {
            return KeywordGroup.containsWord(searchExpression, content);
        }
        return KeywordGroup.containsWord(searchExpression.toLowerCase(), content.toLowerCase());
    }

    /**
     * Look for the given non-regexp string in another string, but check whether a
     * match concerns a complete word, not part of a word.
     *
     * @param word The word to look for.
     * @param text The string to look in.
     * @return true if the word was found, false otherwise.
     */
    private static boolean containsWord(String word, String text) {
        int piv = 0;
        while (piv < text.length()) {
            int index = text.indexOf(word, piv);
            if (index < 0) {
                return false;
            }
            // Found a match. See if it is a complete word:
            if ((index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1))) &&
                    (((index + word.length()) == text.length())
                            || !Character.isLetterOrDigit(text.charAt(index + word.length())))) {
                return true;
            } else {
                piv = index + 1;
            }
        }
        return false;
    }

    /**
     * Removes matches of searchString in the entry's field. This is only
     * possible if the search expression is not a regExp.
     */
    private void removeMatches(BibtexEntry entry) {
        String content = entry.getField(searchField);
        if (content == null) {
            return; // nothing to modify
        }
        StringBuffer sbOrig = new StringBuffer(content);
        StringBuffer sbLower = new StringBuffer(content.toLowerCase());
        StringBuffer haystack = caseSensitive ? sbOrig : sbLower;
        String needle = caseSensitive ? searchExpression
                : searchExpression.toLowerCase();
        int i, j, k;
        final String separator = Globals.prefs.get(JabRefPreferences.GROUP_KEYWORD_SEPARATOR);
        while ((i = haystack.indexOf(needle)) >= 0) {
            sbOrig.replace(i, i + needle.length(), "");
            sbLower.replace(i, i + needle.length(), "");
            // reduce spaces at i to 1
            j = i;
            k = i;
            while (((j - 1) >= 0) && (separator.indexOf(haystack.charAt(j - 1)) >= 0)) {
                --j;
            }
            while ((k < haystack.length()) && (separator.indexOf(haystack.charAt(k)) >= 0)) {
                ++k;
            }
            sbOrig.replace(j, k, (j >= 0) && (k < sbOrig.length()) ? separator : "");
            sbLower.replace(j, k, (j >= 0) && (k < sbOrig.length()) ? separator : "");
        }

        String result = sbOrig.toString().trim();
        entry.setField(searchField, (!result.isEmpty() ? result : null));
    }

    @Override
    public AbstractGroup deepCopy() {
        try {
            return new KeywordGroup(name, searchField, searchExpression,
                    caseSensitive, regExp, context);
        } catch (Throwable t) {
            // this should never happen, because the constructor obviously
            // succeeded in creating _this_ instance!
            System.err.println("Internal error: Exception " + t
                    + " in KeywordGroup.deepCopy(). "
                    + "Please report this on www.sf.net/projects/jabref");
            return null;
        }
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isRegExp() {
        return regExp;
    }

    public String getSearchExpression() {
        return searchExpression;
    }

    public String getSearchField() {
        return searchField;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public String getDescription() {
        return KeywordGroup.getDescriptionForPreview(searchField, searchExpression, caseSensitive, regExp);
    }

    public static String getDescriptionForPreview(String field, String expr, boolean caseSensitive, boolean regExp) {
        String header = regExp ? Globals.lang(
                "This group contains entries whose <b>%0</b> field contains the regular expression <b>%1</b>",
                field, StringUtil.quoteForHTML(expr))
                : Globals.lang(
                "This group contains entries whose <b>%0</b> field contains the keyword <b>%1</b>",
                field, StringUtil.quoteForHTML(expr));
        String caseSensitiveText = caseSensitive ? Globals.lang("case sensitive") : Globals.lang("case insensitive");
        String footer = regExp ?
                Globals.lang("Entries cannot be manually assigned to or removed from this group.")
                : Globals.lang(
                "Additionally, entries whose <b>%0</b> field does not contain "
                        + "<b>%1</b> can be assigned manually to this group by selecting them "
                        + "then using either drag and drop or the context menu. "
                        + "This process adds the term <b>%1</b> to "
                        + "each entry's <b>%0</b> field. "
                        + "Entries can be removed manually from this group by selecting them "
                        + "then using the context menu. "
                        + "This process removes the term <b>%1</b> from "
                        + "each entry's <b>%0</b> field.",
                field, StringUtil.quoteForHTML(expr));

        return String.format("%s (%s). %s", header, caseSensitiveText, footer);
    }

    @Override
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>");
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_DYNAMIC)) {
            sb.append("<i>").append(StringUtil.quoteForHTML(getName())).append("</i>");
        } else {
            sb.append(StringUtil.quoteForHTML(getName()));
        }
        sb.append("</b> - ");
        sb.append(Globals.lang("dynamic group"));
        sb.append("<b>");
        sb.append(searchField);
        sb.append("</b>");
        sb.append(Globals.lang("contains"));
        sb.append(" <b>");
        sb.append(StringUtil.quoteForHTML(searchExpression));
        sb.append("</b>)");
        switch (getHierarchicalContext()) {
            case INCLUDING:
                sb.append(", ").append(Globals.lang("includes subgroups"));
                break;
            case REFINING:
                sb.append(", ").append(Globals.lang("refines supergroup"));
                break;
            default:
                break;
        }
        return sb.toString();
    }

    @Override
    public String getTypeId() {
        return KeywordGroup.ID;
    }
}
