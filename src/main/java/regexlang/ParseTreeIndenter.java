package regexlang;
import org.antlr.v4.runtime.tree.RuleNode;

import regexlang.LispParseTreeParser.*;

public class ParseTreeIndenter extends LispParseTreeBaseVisitor<String> {
  private int indentLevel = 0;

  @Override
  public String visitChildren(RuleNode node) {
    indentLevel++;
    for (int i = 0; i < node.getChildCount(); i++) {
      visit(node.getChild(i));
    }
    indentLevel--;
    return node.getText();
  }

  @Override
  public String visitGroup(GroupContext ctx) {
    StringBuilder sb = new StringBuilder();
    sb.append(" ".repeat(indentLevel));
    sb.append("(");
    sb.append(ctx.KEYWORD_GROUP().getText());
    sb.append(visit(ctx.openGroup()));
    indentLevel++;
    sb.append("\n");
    sb.append(visit(ctx.body()));
    return sb.toString();
  }

  @Override
  public String visitOpenGroup(OpenGroupContext ctx) {
    return "(" + ctx.KEYWORD_OPEN_GROUP().getText() + ")";
  }

  @Override
  public String visitCloseGroup(CloseGroupContext ctx) {
    return "(" + ctx.KEYWORD_CLOSE_GROUP().getText() + ")";
  }

  @Override
  public String visitBody(BodyContext ctx) {
    StringBuilder sb = new StringBuilder();
    sb.append(" ".repeat(indentLevel));
    sb.append("(");
    sb.append(ctx.KEYWORD_BODY().getText());
    sb.append("\n");
    indentLevel++;
    sb.append("\n");
    return sb.toString();
  }

}
