/* Generated By:JavaCC: Do not edit this line. LTAGTreeParser.java */
package org.dllearner.algorithm.tbsl.ltag.reader;

import java.util.ArrayList;
import java.util.List;

import org.dllearner.algorithm.tbsl.ltag.agreement.Feature;
import org.dllearner.algorithm.tbsl.ltag.data.Category;
import org.dllearner.algorithm.tbsl.ltag.data.FootNode;
import org.dllearner.algorithm.tbsl.ltag.data.SubstNode;
import org.dllearner.algorithm.tbsl.ltag.data.TerminalNode;
import org.dllearner.algorithm.tbsl.ltag.data.Tree;
import org.dllearner.algorithm.tbsl.ltag.data.TreeNode;

public class LTAGTreeParser implements LTAGTreeParserConstants {

  /** Main entry point. */
  public static void main(String args[]) throws ParseException {
    LTAGTreeParser parser = new LTAGTreeParser(System.in);
    parser.Input();
  }

/** Root production. */
  final public void Input() throws ParseException {
    Tree();
    jj_consume_token(0);
  }

/** Tree */
  final public TreeNode Tree() throws ParseException {
  Category category;
  String terminal = "";
  List<TreeNode> treelist;
  Token word;
  Feature feature = null;
    if (jj_2_5(5)) {
      // SubstNode with case constraints (e.g. DP[subj]|nom)
         category = Cat();
      jj_consume_token(1);
      word = jj_consume_token(WORD);
      jj_consume_token(2);
      if (jj_2_1(5)) {
        jj_consume_token(3);
        feature = Feat();
        jj_consume_token(4);
      } else {
        ;
      }
        SubstNode substnode = new SubstNode(word.toString(),category,feature);
        {if (true) return substnode;}
    } else if (jj_2_6(5)) {
      // FootNode (e.g. S*)
         category = Cat();
      jj_consume_token(5);
        FootNode footnode = new FootNode(category);
        {if (true) return footnode;}
    } else if (jj_2_7(5)) {
      jj_consume_token(6);
      category = Cat();
      jj_consume_token(5);
        FootNode footnode = new FootNode(category);
        footnode.setAdjConstraint(true);
        {if (true) return footnode;}
    } else if (jj_2_8(5)) {
      jj_consume_token(7);
      category = Cat();
      if (jj_2_2(5)) {
        jj_consume_token(3);
        feature = Feat();
        jj_consume_token(4);
      } else {
        ;
      }
      treelist = TreeList();
      jj_consume_token(8);
       TreeNode tree = new Tree();
       tree.setCategory(category);
       tree.setChildren(treelist);
       tree.setParentForTree();
       tree.setFeature(feature);
       {if (true) return tree;}
    } else if (jj_2_9(5)) {
      jj_consume_token(7);
      jj_consume_token(6);
      category = Cat();
      treelist = TreeList();
      jj_consume_token(8);
       TreeNode tree = new Tree();
       tree.setCategory(category);
       tree.setChildren(treelist);
       tree.setParentForTree();
       tree.setAdjConstraint(true);
       {if (true) return tree;}
    } else if (jj_2_10(5)) {
      // TerminalNode with case feature (e.g. N|nom:'house')
         category = Cat();
      if (jj_2_3(5)) {
        jj_consume_token(3);
        feature = Feat();
        jj_consume_token(4);
      } else {
        ;
      }
      jj_consume_token(9);
      jj_consume_token(10);
      if (jj_2_4(5)) {
        terminal = Terminal();
      } else {
        ;
      }
      jj_consume_token(10);
        TerminalNode node = new TerminalNode(terminal, category);
        node.setCategory(category);
        node.setFeature(feature);
        {if (true) return node;}
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public String Terminal() throws ParseException {
  Token word;
  String terminal=null;
    word = jj_consume_token(WORD);
    if (jj_2_11(5)) {
      terminal = Terminal();
    } else {
      ;
    }
       if (terminal != null) {if (true) return word.toString() + " " + terminal;}
       {if (true) return word.toString();}
    throw new Error("Missing return statement in function");
  }

  final public List<TreeNode> TreeList() throws ParseException {
   List<TreeNode> treelist = null;
   TreeNode tree;
    tree = Tree();
    if (jj_2_12(5)) {
      treelist = TreeList();
    } else {
      ;
    }
        if (treelist == null)
        {
          treelist = new ArrayList<TreeNode>();
        }

        treelist.add(0,tree);
        {if (true) return treelist;}
    throw new Error("Missing return statement in function");
  }

  final public Category Cat() throws ParseException {
  Token cat;
    cat = jj_consume_token(CATEGORY);
      if (cat.toString().equals("DP")) {if (true) return Category.DP;}
      if (cat.toString().equals("NP")) {if (true) return Category.NP;}
      if (cat.toString().equals("N")) {if (true) return Category.N;}
      if (cat.toString().equals("S")) {if (true) return Category.S;}
      if (cat.toString().equals("V")) {if (true) return Category.V;}
      if (cat.toString().equals("P")) {if (true) return Category.P;}
      if (cat.toString().equals("VP")) {if (true) return Category.VP;}
      if (cat.toString().equals("PP")) {if (true) return Category.PP;}
      if (cat.toString().equals("DET")) {if (true) return Category.DET;}
      if (cat.toString().equals("WH")) {if (true) return Category.WH;}
      if (cat.toString().equals("ADV")) {if (true) return Category.ADV;}
      if (cat.toString().equals("ADJ")) {if (true) return Category.ADJ;}
      if (cat.toString().equals("ADJCOMP")) {if (true) return Category.ADJCOMP;}
      if (cat.toString().equals("PART")) {if (true) return Category.PART;}
      if (cat.toString().equals("PUNCT")) {if (true) return Category.PUNCT;}
      if (cat.toString().equals("CC")) {if (true) return Category.CC;}
      if (cat.toString().equals("EX")) {if (true) return Category.EX;}
      if (cat.toString().equals("NUM")) {if (true) return Category.NUM;}
      if (cat.toString().equals("C")) {if (true) return Category.C;}
      if (cat.toString().equals("NEG")) {if (true) return Category.NEG;}
    throw new Error("Missing return statement in function");
  }

  final public Feature Feat() throws ParseException {
  Token raw;
    raw = jj_consume_token(WORD);
        {if (true) return Feature.construct(raw.toString());}
    throw new Error("Missing return statement in function");
  }

  private boolean jj_2_1(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_1(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  private boolean jj_2_2(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_2(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(1, xla); }
  }

  private boolean jj_2_3(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_3(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(2, xla); }
  }

  private boolean jj_2_4(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_4(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(3, xla); }
  }

  private boolean jj_2_5(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_5(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(4, xla); }
  }

  private boolean jj_2_6(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_6(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(5, xla); }
  }

  private boolean jj_2_7(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_7(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(6, xla); }
  }

  private boolean jj_2_8(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_8(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(7, xla); }
  }

  private boolean jj_2_9(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_9(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(8, xla); }
  }

  private boolean jj_2_10(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_10(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(9, xla); }
  }

  private boolean jj_2_11(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_11(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(10, xla); }
  }

  private boolean jj_2_12(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_12(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(11, xla); }
  }

  private boolean jj_3R_5() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_5()) {
    jj_scanpos = xsp;
    if (jj_3_6()) {
    jj_scanpos = xsp;
    if (jj_3_7()) {
    jj_scanpos = xsp;
    if (jj_3_8()) {
    jj_scanpos = xsp;
    if (jj_3_9()) {
    jj_scanpos = xsp;
    if (jj_3_10()) return true;
    }
    }
    }
    }
    }
    return false;
  }

  private boolean jj_3_5() {
    if (jj_3R_3()) return true;
    if (jj_scan_token(1)) return true;
    if (jj_scan_token(WORD)) return true;
    if (jj_scan_token(2)) return true;
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_1()) jj_scanpos = xsp;
    return false;
  }

  private boolean jj_3_9() {
    if (jj_scan_token(7)) return true;
    if (jj_scan_token(6)) return true;
    if (jj_3R_3()) return true;
    if (jj_3R_4()) return true;
    return false;
  }

  private boolean jj_3_7() {
    if (jj_scan_token(6)) return true;
    if (jj_3R_3()) return true;
    if (jj_scan_token(5)) return true;
    return false;
  }

  private boolean jj_3R_4() {
    if (jj_3R_5()) return true;
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_12()) jj_scanpos = xsp;
    return false;
  }

  private boolean jj_3_11() {
    if (jj_3R_2()) return true;
    return false;
  }

  private boolean jj_3_1() {
    if (jj_scan_token(3)) return true;
    if (jj_3R_1()) return true;
    if (jj_scan_token(4)) return true;
    return false;
  }

  private boolean jj_3_4() {
    if (jj_3R_2()) return true;
    return false;
  }

  private boolean jj_3R_3() {
    if (jj_scan_token(CATEGORY)) return true;
    return false;
  }

  private boolean jj_3_12() {
    if (jj_3R_4()) return true;
    return false;
  }

  private boolean jj_3_6() {
    if (jj_3R_3()) return true;
    if (jj_scan_token(5)) return true;
    return false;
  }

  private boolean jj_3_10() {
    if (jj_3R_3()) return true;
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_3()) jj_scanpos = xsp;
    if (jj_scan_token(9)) return true;
    if (jj_scan_token(10)) return true;
    xsp = jj_scanpos;
    if (jj_3_4()) jj_scanpos = xsp;
    if (jj_scan_token(10)) return true;
    return false;
  }

  private boolean jj_3_8() {
    if (jj_scan_token(7)) return true;
    if (jj_3R_3()) return true;
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_2()) jj_scanpos = xsp;
    if (jj_3R_4()) return true;
    if (jj_scan_token(8)) return true;
    return false;
  }

  private boolean jj_3_2() {
    if (jj_scan_token(3)) return true;
    if (jj_3R_1()) return true;
    if (jj_scan_token(4)) return true;
    return false;
  }

  private boolean jj_3_3() {
    if (jj_scan_token(3)) return true;
    if (jj_3R_1()) return true;
    if (jj_scan_token(4)) return true;
    return false;
  }

  private boolean jj_3R_2() {
    if (jj_scan_token(WORD)) return true;
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_11()) jj_scanpos = xsp;
    return false;
  }

  private boolean jj_3R_1() {
    if (jj_scan_token(WORD)) return true;
    return false;
  }

  /** Generated Token Manager. */
  public LTAGTreeParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  private int jj_gen;
  final private int[] jj_la1 = new int[0];
  static private int[] jj_la1_0;
  static {
      jj_la1_init_0();
   }
   private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {};
   }
  final private JJCalls[] jj_2_rtns = new JJCalls[12];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  /** Constructor with InputStream. */
  public LTAGTreeParser(java.io.InputStream stream) {
     this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public LTAGTreeParser(java.io.InputStream stream, String encoding) {
    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new LTAGTreeParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor. */
  public LTAGTreeParser(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new LTAGTreeParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor with generated Token Manager. */
  public LTAGTreeParser(LTAGTreeParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(LTAGTreeParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  static private final class LookaheadSuccess extends java.lang.Error { }
  final private LookaheadSuccess jj_ls = new LookaheadSuccess();
  private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0; Token tok = token;
      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }


/** Get the next Token. */
  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;

  private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      jj_entries_loop: for (java.util.Iterator<?> it = jj_expentries.iterator(); it.hasNext();) {
        int[] oldentry = (int[])(it.next());
        if (oldentry.length == jj_expentry.length) {
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              continue jj_entries_loop;
            }
          }
          jj_expentries.add(jj_expentry);
          break jj_entries_loop;
        }
      }
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  /** Generate ParseException. */
  public ParseException generateParseException() {
    jj_expentries.clear();
    boolean[] la1tokens = new boolean[17];
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 0; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 17; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.add(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = jj_expentries.get(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

  private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 12; i++) {
    try {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0: jj_3_1(); break;
            case 1: jj_3_2(); break;
            case 2: jj_3_3(); break;
            case 3: jj_3_4(); break;
            case 4: jj_3_5(); break;
            case 5: jj_3_6(); break;
            case 6: jj_3_7(); break;
            case 7: jj_3_8(); break;
            case 8: jj_3_9(); break;
            case 9: jj_3_10(); break;
            case 10: jj_3_11(); break;
            case 11: jj_3_12(); break;
          }
        }
        p = p.next;
      } while (p != null);
      } catch(LookaheadSuccess ls) { }
    }
    jj_rescan = false;
  }

  private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) { p = p.next = new JJCalls(); break; }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

}