package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.Pair;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.NullNode;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EnumJ extends Value {
    public String name;
    Map<String, EnumJChild> children;
    public EnumJ(String name, Map<String, EnumJChild> children) {
        this.name = name;
        this.children = children;

        Set<String> keySet = children.keySet();
        for (String key : keySet)
            children.get(key).setParent(this);

        jptype = Constants.JPType.Enum;
    }

    // Functions

    public EnumJChild getChild(String name) {
        return children.get(name);
    }

    // Methods

    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != Constants.JPType.Enum) return new Pair<>(new Bool(false), null);
        EnumJ other = (EnumJ) o;
        return new Pair<>(new Bool(other.name.equals(name)), null);
    }

    // Conversions

    public Obj dictionary() {
        Value thisaround = this;
        return new Dict(new HashMap<>(){{ put(thisaround, thisaround); }})
                .set_context(context).set_pos(pos_start, pos_end);
    }

    public Obj alist() {
        Value thisaround = this;
        return new PList(new ArrayList<>() {{ add(thisaround); }}).set_context(context).set_pos(pos_start, pos_end);
    }

    public Obj number() {
        return new Num(children.size())
            .set_context(context).set_pos(pos_start, pos_end);
    }

    public Obj bool() { return new Bool(children.size() > 0).set_context(context).set_pos(pos_start, pos_end); }

    public Obj function() {
        return new Function(null, new NullNode(new Token(Tokens.TT.KEYWORD, "null")), null)
            .set_context(context).set_pos(pos_start, pos_end);
    }

    // Defaults

    public String toString() { return name; }
    public Obj type() { return new Str("Enum").set_context(context).set_pos(pos_start, pos_end); }
    public Obj copy() { return new EnumJ(name, children).set_context(context).set_pos(pos_start, pos_end); }

}