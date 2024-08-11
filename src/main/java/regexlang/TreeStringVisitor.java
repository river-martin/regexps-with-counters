package regexlang;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import regexlang.SimpleRegexpParser.*;
import org.antlr.v4.runtime.*;

/**
 * Replace quantifiers, character classes, and groups with terminal nodes
 * containing their text
 */
public class TreeStringVisitor extends SimpleRegexpBaseVisitor<ParseTree> {

  /**
   * Visit all children of a node and return the node itself.
   * 
   * @param node The node to visit.
   * @return The node itself.
   */
  @Override
  public ParseTree visitChildren(RuleNode node) {
    super.visitChildren(node);
    return node;
  }

  /**
   * Visit a quantifier node and replace it with a terminal node containing the
   * quantifier text.
   */
  @Override
  public ParseTree visitQuantifier(QuantifierContext ctx) {
    visitChildren(ctx);
    CommonToken token = new CommonToken(-1, ctx.getText());
    TerminalNodeImpl terminal = new TerminalNodeImpl(token);
    return terminal;
  }

  @Override
  public ParseTree visitCharCls(CharClsContext ctx) {
    visitChildren(ctx);
    CommonToken token = new CommonToken(-1, ctx.getText());
    TerminalNodeImpl terminal = new TerminalNodeImpl(token);
    return terminal;
  }

  @Override
  public ParseTree visitGroup(GroupContext ctx) {
    visitChildren(ctx);
    CommonToken token = new CommonToken(-1, ctx.getChild(0).getText());
    TerminalNodeImpl terminal = new TerminalNodeImpl(token);
    return terminal;
  }

  public static ParseTree rewriteWithTokensForParserRules(ParseTree tree) {
    TreeStringVisitor visitor = new TreeStringVisitor();
    return visitor.visit(tree);
  }

}
