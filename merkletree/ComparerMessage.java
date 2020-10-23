package merkletree;

import java.util.ArrayList;
import java.util.List;
import merkletree.Leaf;

public  class ComparerMessage
{
    //枚举类型表示树和主树相比同步需要进行的操作
    enum messagetype{EQUAL,MODIFY,ADD,DELETE};
    private  List<Leaf> leafs=null;
    private  messagetype op=messagetype.EQUAL;
    private String filename=null;

    public ComparerMessage() { }

    public void setFilename(String filename){this.filename = filename;}

    public String getFilename(){return this.filename;}

    public messagetype getop(){return op;}

    public void setop(messagetype OP){ op = OP;}

    public List<Leaf> getLeafs(){return leafs;}

    public void setLeafs(List<Leaf> Leafs){ leafs = Leafs;}
}