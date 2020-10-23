package merkletree;


import java.util.ArrayList;
import java.util.List;


import merkletree.Leaf;
import merkletree.MerkleTree;
import merkletree.TreeBuilder;
import merkletree.ComparerMessage;
import org.jetbrains.annotations.NotNull;

public class comparer
    {
        //比较两个叶节点的block是否相同
        private static boolean equalblock(final List<byte[]> a1,final List<byte[]> a2)
        {

            if(a1.size() != a2.size())
                 return false;
            for(int i = 0; i < a1.size(); i++)
                {
                    if(a1.get(i).length != a2.get(i).length) {
                        //System.out.println("length not match");
                        return false;
                    }
                    else {
                        for (int idx = 0; idx < a1.get(i).length; idx++)
                            if (a1.get(i)[idx] != a2.get(i)[idx]) {
                                return false;
                            }
                    }
                }
            return true;
        }

        //比较普通节点的digest值。
        private static boolean equaldigest(final byte[] a,final byte[] b)
        {

            if(a.length != b.length)
                return false;
            else {
                for (int idx = 0; idx < a.length; idx++)
                    if (a[idx] != b[idx])
                        return false;
            }
            return true;
        }

        //搜索树，并加入除了blocks之外的叶节点。
        private static void search(final @NotNull MerkleTree tree, final MerkleTree tree2, List<Leaf> leafs)
        {
            //检查左叶节点
            if (tree.rightLeaf()!=null && !equalblock(tree2.leftLeaf().getDataBlock(),tree.rightLeaf().getDataBlock())
            && !equalblock(tree2.rightLeaf().getDataBlock(),tree.rightLeaf().getDataBlock()))
                leafs.add(tree.rightLeaf());
            //检查右叶节点
            if (tree.leftLeaf()!=null && !equalblock(tree2.leftLeaf().getDataBlock(),tree.leftLeaf().getDataBlock())
            && !equalblock(tree2.rightLeaf().getDataBlock(),tree.leftLeaf().getDataBlock()))
                leafs.add(tree.leftLeaf());
            //递归检查左子树
            if(tree.leftTree()!=null)
                search(tree.leftTree(),tree2,leafs);
            //递归检查右子树
            if(tree.rightTree()!=null)
                search(tree.rightTree(),tree2,leafs);
        }


        //p1是主树，p2向p1对齐
        public static ComparerMessage TreeCompare(final MerkleTree tree1,final MerkleTree tree2)
        {
            //暂时一次只做一次改变

            ComparerMessage message= new ComparerMessage();
            ComparerMessage.messagetype op;
            List<Leaf> leafs = new ArrayList<Leaf>();

            //两个树根节点的哈希值相同，树相同，直接返回相同
            if(equaldigest(tree1.digest(),tree2.digest()))
            {
                //System.out.println("equal!");
                op= ComparerMessage.messagetype.EQUAL;
                message.setop(op);
                return message;
            }
            //如果根节点不相同，观察其子节点
            //节点的不同有首先树的结构相同，但是某个叶节点的block内容不同（修改需求）
            //还有树的结构不同，叶节点数目更多（目录中多文件，删除）或者更少（少文件，添加）

            MerkleTree p1=tree1;
            MerkleTree p2=tree2;


            while(true)
            {
               //当主树节点不是叶节点，如果从树节点是叶节点，说明主树节点文件更多
               if((p2.leftLeaf()!=null && p1.leftTree()!=null))
               {
                   //主树节点是树，从树孩子是叶节点
                   op = ComparerMessage.messagetype.ADD;
                   if(!equalblock(p2.leftLeaf().getDataBlock(),p1.leftTree().leftLeaf().getDataBlock()))
                       leafs.add(p1.leftTree().leftLeaf());
                   else leafs.add(p1.leftTree().rightLeaf());
                   message.setop(op);
                   message.setLeafs(leafs);
                    return message;
               }
               if(p2.rightLeaf()!=null && p1.rightTree()!=null)
               {   op = ComparerMessage.messagetype.ADD;
                   if(!equalblock(p2.rightLeaf().getDataBlock(),p1.rightTree().leftLeaf().getDataBlock()))
                       leafs.add(p1.rightTree().leftLeaf());
                   else leafs.add(p1.rightTree().rightLeaf());
                   //search(p1,p2,leafs);
                   message.setop(op);
                   message.setLeafs(leafs);
                   return message;

               }
               //如果当前访问的主树节点是叶节点，从树节点还是树节点，则说明从树节点文件更多
               if((p1.leftLeaf()!=null && p2.leftTree()!=null) ||
                       (p1.rightLeaf()!=null && p2.rightTree()!=null))
                    {
                        //主树的左节点是叶节点，从树的左节点是树
                        op = ComparerMessage.messagetype.DELETE;
                        //遍历p2的左子树，加入不等于p1.leftLeaf()的叶节点
                        search(p2,p1,leafs);
                        message.setop(op);
                        message.setLeafs(leafs);
                        return message;
                    }
                //上面四种就是类型左右类型不一致的，接下来都是左右类型一致的

                //左右孩子有叶节点的情况
               if (p1.leftLeaf() != null && p1.rightLeaf() != null) {
                     if(!equalblock(p1.leftLeaf().getDataBlock(),p2.leftLeaf().getDataBlock()))
                           {
                                op = ComparerMessage.messagetype.MODIFY;
                                message.setop(op);
                                leafs.add(p1.leftLeaf());
                                leafs.add(p2.leftLeaf());
                                message.setLeafs(leafs);
                                return message;
                           }
                     else{
                          op = ComparerMessage.messagetype.MODIFY;
                          message.setop(op);
                          leafs.add(p1.rightLeaf());
                          leafs.add(p2.rightLeaf());
                          message.setLeafs(leafs);
                          return message;
                     }
               }
               //只有左节点是叶节点
               if (p1.leftLeaf() != null) {
                     //树的结构相同，只是p2的一个叶节点和p1不同，返回p1的叶节点同步
                     if (!equalblock(p1.leftLeaf().getDataBlock(), p2.leftLeaf().getDataBlock())) {
                          op = ComparerMessage.messagetype.MODIFY;
                          message.setop(op);
                          leafs.add(p1.leftLeaf());
                          leafs.add(p2.leftLeaf());
                          message.setLeafs(leafs);
                          return message;
                     } else {
                         p1 = p1.rightTree();
                         p2 = p2.rightTree();
                         continue;
                     }
               } else if(p1.rightLeaf() != null){
                   if (!equalblock(p1.rightLeaf().getDataBlock(), p2.rightLeaf().getDataBlock())) {
                       op = ComparerMessage.messagetype.MODIFY;
                       message.setop(op);
                       leafs.add(p1.rightLeaf());
                       leafs.add(p2.rightLeaf());
                       message.setLeafs(leafs);
                       return message;
                   } else {
                       p1 = p1.leftTree();
                       p2 = p2.rightTree();
                       continue;
                   }
               }

                        //左右孩子都是树节点
                if (!equaldigest(p1.rightTree().digest(), p2.rightTree().digest())) {
                    p1 = p1.rightTree();
                    p2 = p2.rightTree();
                } else {
                    p1 = p1.leftTree();
                    p2 = p2.leftTree();
                }

            }
        }
}
