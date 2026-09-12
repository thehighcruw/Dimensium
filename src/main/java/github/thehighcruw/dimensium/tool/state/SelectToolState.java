package github.thehighcruw.dimensium.tool.state;

public class SelectToolState {

    public static final SelectToolState INSTANCE = new SelectToolState();

    public SelectionMode selectionMode = SelectionMode.BOX;
    public BooleanOp booleanOp = BooleanOp.ADD;
}
