package merkletree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TreesynDir {
    public static Leaf searchfile(String filename,MerkleTree tree){
        //使用二分查找
        if(tree.leftTree() != null && tree.leftTree().getFilelists().contains(filename))
            return searchfile(filename,tree.leftTree());
        if(tree.rightTree() != null)
            return searchfile(filename,tree.rightTree());

        if(tree.leftLeaf() != null)
        {
            if(tree.leftLeaf().getFilename().equals(filename))
                return tree.leftLeaf();
        }
        if(tree.rightLeaf() != null)
        {
            if(tree.rightLeaf().getFilename().equals(filename))
                return tree.rightLeaf();
        }
        return null;
    }

    public static ComparerMessage FindChange(String dir, MerkleTree tree) throws IOException {
        ComparerMessage message = new ComparerMessage();

        //查找目录和当前目录生成的树之间是否有差别
        File[] filelist = new File(dir).listFiles();
        List<String> filenamelist = new ArrayList<String>();
        for(File file:filelist) {
            filenamelist.add(file.getName());
        }

        if(filenamelist.containsAll(tree.getFilelists())
                && tree.getFilelists().containsAll(filenamelist)) {
            //文件名全部相同，检查文件内容，看是否文件内容改变,查找所有叶节点，比对Hash值
            for(String filename:filenamelist)
            {
                //System.out.println("search "+filename);
                Leaf leaf = searchfile(filename,tree);
                File file = new File(dir+"\\"+filename);
                BufferedReader br = new BufferedReader(new FileReader(file.getPath()));
                String content = br.readLine();
                br.close();

                assert leaf != null;
                //System.out.println(leaf.toString());
                String leafcontext = new String(leaf.getDataBlock().get(1));
                if(!leafcontext.equals(content))
                {
                    //直接修改值
                    //System.out.println(leafcontext);
                    //System.out.println(filename+" " +content);
                    byte[] block1 = filename.getBytes();
                    byte[] block2 = new byte[0];
                    try {
                        block2 = content.getBytes();
                    }catch (Exception e){

                    }
                    List<byte[]> blocks = new ArrayList<byte[]>();
                    blocks.add(block1);
                    blocks.add(block2);
                    leaf.setDataBlock(blocks);
                    //看leaf是左节点还是右节点
                    MerkleTree father = leaf.getFather();
                    if(father.leftLeaf()== leaf)
                    {
                        if(father.rightLeaf()!=null)
                            father.add(leaf,father.rightLeaf());
                        else father.add(leaf,father.rightTree());
                    }else{
                        if(father.leftLeaf()!=null)
                            father.add(father.leftLeaf(),leaf);
                        else father.add(father.leftTree(),leaf);
                    }
                    MerkleTree node = father;
                    father = father.getFather();
                    //leaf.getFather().add(leaf,)
                    //向上递归修改值
                    while(father != null)
                    {
                        if(father.leftTree()== node)
                        {
                            if(father.rightLeaf()!=null)
                                father.add(node,father.rightLeaf());
                            else father.add(node,father.rightTree());
                        }else{
                            if(father.leftLeaf()!=null)
                                father.add(father.leftLeaf(),node);
                            else father.add(father.leftTree(),node);
                        }
                        node = father;
                        father = father.getFather();
                    }
                }

            }
            message.setop(ComparerMessage.messagetype.EQUAL);
        }
        else if(filenamelist.containsAll(tree.getFilelists())) {
            filenamelist.removeAll(tree.getFilelists());
            String filename = filenamelist.get(0);
            //System.out.println("dir has "+filename + " which tree doesn't have ");
            message.setop(ComparerMessage.messagetype.ADD);
            message.setFilename(filename);
        }
        else{
            List<String> list = new ArrayList<String>(tree.getFilelists());
            list.removeAll(filenamelist);
            String filename = list.get(0);
            //System.out.println("tree has "+filename + " which dir doesn't have ");
            message.setop(ComparerMessage.messagetype.DELETE);
            message.setFilename(filename);
        }
        //System.out.println(tree.getFilelists());
        return message;
    }

    private static void treeaddleaf(Leaf leaf, MerkleTree merkleTree)
    {
        //在树中增加一个节点，使用BFS，和找到的第一个叶节点结合成一个新的branch,并更改父节点的值直到根节点
        Queue<MerkleTree> q=new LinkedList<>();
        String filename = leaf.getFilename();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            // Should never happen, we specified SHA, a valid algorithm
            assert false;
        }
        q.offer(merkleTree); //把根节点加入队列中
        while(!q.isEmpty()){
            MerkleTree p = q.poll();
            if(p.leftLeaf()!=null)
            {
                //找到第一个叶节点，开始结合
                //首先建立一个新的节点
                MerkleTree branch = new MerkleTree(md);
                branch.add(p.leftLeaf(),leaf);  //建立一个新的节点
                p.leftLeaf().setFather(branch);  //重置叶节点的父指针
                leaf.setFather(branch);
                branch.addfiles(p.leftLeaf().getFilename());  //设置节点的文件列表
                branch.addfiles(filename);
                branch.setFather(p);  //节点设置父节点
                p.setLeftLeaf(null);
                if(p.rightTree()!=null)   //把原来叶节点的位置改成结合的节点
                    p.add(branch,p.rightTree());
                else p.add(branch,p.rightLeaf());
                p.addfiles(filename);
                //回溯修改哈希值和文件列表
                MerkleTree father = p.getFather();
                while(father != null)
                {
                    father.addfiles(filename);
                    if(father.leftTree()== p)
                    {
                        if(father.rightLeaf()!=null)
                            father.add(p,father.rightLeaf());
                        else father.add(p,father.rightTree());
                    }else{
                        if(father.leftLeaf()!=null)
                            father.add(father.leftLeaf(),p);
                        else father.add(father.leftTree(),p);
                    }
                    p = father;
                    father = father.getFather();
                }
                return;
            }
            if(p.rightLeaf()!=null)
            {
                //找到第一个叶节点，开始结合
                //首先建立一个新的节点
                MerkleTree branch = new MerkleTree(md);
                branch.add(leaf, p.rightLeaf());  //建立一个新的节点
                p.rightLeaf().setFather(branch);  //重置叶节点的父指针
                leaf.setFather(branch);
                branch.addfiles(p.rightLeaf().getFilename());  //设置节点的文件列表
                branch.addfiles(filename);
                branch.setFather(p);  //节点设置父节点
                p.setRightLeaf(null);
                if(p.leftTree()!=null)   //把原来叶节点的位置改成结合的节点
                    p.add(p.leftTree(),branch);
                else p.add(p.rightTree(),branch);
                p.addfiles(filename);
                //回溯修改哈希值和文件列表
                MerkleTree father = p.getFather();
                while(father != null)
                {
                    father.addfiles(filename);
                    if(father.leftTree()== p)
                    {
                        if(father.rightLeaf()!=null)
                            father.add(p,father.rightLeaf());
                        else father.add(p,father.rightTree());
                    }else{
                        if(father.leftLeaf()!=null)
                            father.add(father.leftLeaf(),p);
                        else father.add(father.leftTree(),p);
                    }
                    p = father;
                    father = father.getFather();
                }
                return;
            }
            if(p.leftTree() != null)
                q.offer(p.leftTree());
            if(p.rightTree()!=null)
                q.offer(p.rightTree());
        }
        System.out.println("成功在树中添加节点");
    }


    private static void treedeleteleaf(Leaf leaf,MerkleTree merkleTree) {
        //在树中删除一个节点，找到该节点并删除，兄弟节点直接取代父节点的位置
        //还需要修改维护文件名列表
        MerkleTree father = leaf.getFather();
        MerkleTree grandfather = father.getFather();
        String filename = leaf.getFilename();
        father.deletefile(filename);
        grandfather.deletefile(filename);


        if (father.leftLeaf() == leaf) {
            //右节点上提
            if (father.rightLeaf() != null) {
                //兄弟节点也是叶子节点
                if (grandfather.leftTree() == father) {
                    //父节点是祖父节点的左子树,用兄弟节点来取代祖父节点的左子树
                    if (grandfather.rightTree() != null)
                        grandfather.add(father.rightLeaf(), grandfather.rightTree());
                    else
                        grandfather.add(father.rightLeaf(), grandfather.rightLeaf());
                    grandfather.setLeftTree(null);
                } else {
                    //父节点是祖父节点的右子树,用兄弟节点来取代祖父节点的右子树
                    if (grandfather.leftTree() != null)
                        grandfather.add(grandfather.leftTree(), father.rightLeaf());
                    else
                        grandfather.add(grandfather.leftLeaf(), father.rightLeaf());
                    grandfather.setRightTree(null);
                }
            } else {
                //兄弟节点是树节点
                if (grandfather.leftTree() == father) {
                    //父节点是祖父节点的左子树,用兄弟节点来取代祖父节点的左子树
                    if (grandfather.rightTree() != null)
                        grandfather.add(father.rightTree(), grandfather.rightTree());
                    else
                        grandfather.add(father.rightTree(), grandfather.rightLeaf());
                } else {
                    //父节点是祖父节点的右子树,用兄弟节点来取代祖父节点的右子树
                    if (grandfather.leftTree() != null)
                        grandfather.add(grandfather.leftTree(), father.rightTree());
                    else
                        grandfather.add(grandfather.leftLeaf(), father.rightTree());
                }
            }
        } else {
            //左节点上提
            if (father.leftLeaf() != null) {
                //兄弟节点也是叶子节点
                if (grandfather.leftTree() == father) {
                    //父节点是祖父节点的左子树,用兄弟节点来取代祖父节点的左子树
                    if (grandfather.rightTree() != null)
                        grandfather.add(father.leftLeaf(), grandfather.rightTree());
                    else
                        grandfather.add(father.leftLeaf(), grandfather.rightLeaf());
                    grandfather.setLeftTree(null);
                } else {
                    //父节点是祖父节点的右子树,用兄弟节点来取代祖父节点的右子树
                    if (grandfather.leftTree() != null)
                        grandfather.add(grandfather.leftTree(), father.leftLeaf());
                    else
                        grandfather.add(grandfather.leftLeaf(), father.leftLeaf());
                    grandfather.setRightTree(null);
                }
            } else {
                //兄弟节点是树节点
                if (father.getFather().leftTree() == father) {
                    //父节点是祖父节点的左子树,用兄弟节点来取代祖父节点的左子树
                    if (grandfather.rightTree() != null)
                        grandfather.add(father.leftTree(), grandfather.rightTree());
                    else
                        grandfather.add(father.leftTree(), grandfather.rightLeaf());
                } else {
                    //父节点是祖父节点的右子树,用兄弟节点来取代祖父节点的右子树
                    if (grandfather.leftTree() != null)
                        grandfather.add(grandfather.leftTree(), father.leftTree());
                    else
                        grandfather.add(grandfather.leftLeaf(), father.leftTree());
                }
            }
        }
        //回溯更新哈希值
        father = grandfather;
        grandfather = grandfather.getFather();
        while(grandfather != null)
        {
            grandfather.deletefile(filename);
            if(grandfather.leftTree()== father)
            {
                if(grandfather.rightLeaf()!=null)
                    grandfather.add(father,grandfather.rightLeaf());
                else grandfather.add(father,grandfather.rightTree());
            }else{
                if(grandfather.leftLeaf()!=null)
                    grandfather.add(grandfather.leftLeaf(),father);
                else grandfather.add(grandfather.leftTree(),father);
            }
            father = grandfather;
            grandfather = grandfather.getFather();
        }
        System.out.println("Successful delete leaf from tree!");

    }
    static void synchronizationtree(String dir, MerkleTree merkleTree) throws IOException {
        ComparerMessage message = FindChange(dir,merkleTree);
        //本来就同步不需要操作，一个文件被修改不需要操作，因为在判断是否同步时已经对其进行了修改，同步。
        if(message.getop() == ComparerMessage.messagetype.ADD) {
            File file = new File(dir + "\\" + message.getFilename());
            String content;
            try {
                BufferedReader br = new BufferedReader(new FileReader(file.getPath()));
                content = br.readLine();
                br.close();
            }catch(Exception e){
                content = "";
            }
            byte[] block1 = message.getFilename().getBytes();
            List<byte[]> blocks = new ArrayList<byte[]>();
            blocks.add(block1);
            if(!content.equals(""))
            {
                byte[] block2=content.getBytes();
                blocks.add(block2);
            }
            Leaf leaf = new Leaf(blocks,message.getFilename());
            treeaddleaf(leaf,merkleTree);
        }
        else if(message.getop() == ComparerMessage.messagetype.DELETE)
        {
            Leaf leaf = searchfile(message.getFilename(),merkleTree);
            treedeleteleaf(leaf,merkleTree);
        }

    }
}
