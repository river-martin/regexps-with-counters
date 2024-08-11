package regexlang;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;
import org.junit.jupiter.api.Test;

import cli.IterableLines;
import regexlang.SimpleRegexpParser.UnboundedCounterContext;

public class TestQuantExprRewriteVisitor {
  /**
   * Test that unbounded counters are translated correctly.
   */
  @Test
  public void testTranslationOfUnboundedCounters() {
    String fileName = config.Config.getProperty("testInputDir") + "unbounded_counters.txt";
    ParseTree tree;
    for (String line : new IterableLines(fileName)) {
      SimpleRegexpParser parser = QuantExprRewriteVisitor.makeParser(line);
      tree = parser.regexp();
      ParseTree rewrittenTree = QuantExprRewriteVisitor.rewriteUnboundedCounters(tree);
      assert rewrittenTree != null;
      SimpleRegexpBaseVisitor<ParseTree> validationVisitor = new SimpleRegexpBaseVisitor<ParseTree>() {
        @Override
        public ParseTree visitUnboundedCounter(UnboundedCounterContext ctx) {
          // If we get here, the rewriter did not do its job
          assert false;
          return ctx;
        }
      };
      validationVisitor.visit(rewrittenTree);

      // The rewriter should not change the tree if there are no unbounded counters
      // (and there should not be any after the first rewrite)
      ParseTree validationTree = QuantExprRewriteVisitor.rewriteUnboundedCounters(rewrittenTree);
      assert validationTree != null;
      assert validationTree.equals(rewrittenTree);
    }
  }
}
