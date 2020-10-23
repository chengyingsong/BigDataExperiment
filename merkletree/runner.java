package merkletree;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import merkletree.Leaf;
import merkletree.MerkleTree;
import merkletree.TreeBuilder;
import merkletree.comparer;
import merkletree.ComparerMessage;
import merkletree.TreeBuilderDir;
import merkletree.ModifyDir;
import merkletree.TreesynDir;

public  class runner {

    private static boolean compare(String dir1,String dir2,
                                   MerkleTree merkleTree1,MerkleTree merkleTree2) throws IOException {
        TreesynDir.synchronizationtree(dir1,merkleTree1);
        TreesynDir.synchronizationtree(dir2,merkleTree2);

        //两课树进行比较
        ComparerMessage message = new ComparerMessage();
        message = comparer.TreeCompare(merkleTree1, merkleTree2);
        //子节点向主节点同步，包括目录和树
        if (message.getop() == ComparerMessage.messagetype.EQUAL) {
            //System.out.println(dir2+"equal!!");
            return false;
        }
        else if (message.getop() == ComparerMessage.messagetype.MODIFY) {
            System.out.println("modify!!");
            List<Leaf> leafs = message.getLeafs();
            //头一个是主节点中的值，后一个是从节点的
            System.out.println("the modify filename is : " + leafs.get(0).getFilename());
            //进行修改操作，即覆盖文件内容
            ModifyDir.Modify(dir1, dir2, leafs.get(0).getFilename());
            //目录文件同步完，同步树
            TreesynDir.synchronizationtree(dir2,merkleTree2);
            System.out.println("同步目录 "+ dir2);
            return false;
        } else if (message.getop() == ComparerMessage.messagetype.ADD) {
            System.out.println("ADD!!");
            List<Leaf> leafs = message.getLeafs();
            for (Leaf f : leafs) {
                System.out.println("we should add: " + f.getFilename());
                ModifyDir.Add(dir1, dir2, f.getFilename());
                TreesynDir.synchronizationtree(dir2,merkleTree2);
            }
            return true;
        } else if (message.getop() == ComparerMessage.messagetype.DELETE) {
            System.out.println("DELETE!!!");
            List<Leaf> leafs = message.getLeafs();
            for (Leaf f : leafs) {
                System.out.println("we should delete: " + f.getFilename());
                ModifyDir.delete(dir2, f.getFilename());
                TreesynDir.synchronizationtree(dir2,merkleTree2);
            }
            return true;
        }
        return false;
    }

    public static void main(String[] noargs) throws  Exception{

        String dir1 = "D:\\大三上\\大数据处理\\lab3\\merkletree\\dir1";
        String dir2 = "D:\\大三上\\大数据处理\\lab3\\merkletree\\dir2";
        String dir3 = "D:\\大三上\\大数据处理\\lab3\\merkletree\\dir3";
        String dir4 = "D:\\大三上\\大数据处理\\lab3\\merkletree\\dir4";
        TreeBuilderDir builderDir1 = new TreeBuilderDir(dir1);
        MerkleTree merkleTree1 = builderDir1.Builder();
        TreeBuilderDir builderDir2 = new TreeBuilderDir(dir2);
        MerkleTree merkleTree2 = builderDir2.Builder();
        TreeBuilderDir builderDir3 = new TreeBuilderDir(dir3);
        MerkleTree merkleTree3 = builderDir3.Builder();
        TreeBuilderDir builderDir4 = new TreeBuilderDir(dir4);
        MerkleTree merkleTree4 = builderDir4.Builder();

        /*
        merkleTree2.prettyPrint();
        ModifyDir.delete(dir2,"06.txt");
        synchronizationtree(dir2,merkleTree2);
        merkleTree2.prettyPrint();
        ModifyDir.Add(dir1,dir2,"06.txt");
        synchronizationtree(dir2,merkleTree2);
        //修改目录后的树
        merkleTree2.prettyPrint();
        */
         /*1. 首先获取文件目录，查看是否发生改变，如果发生改变修改树
           2. 然后两棵树进行比较，如果不同修改从节点的目录和树
            3. 休眠10s，再次进行循环（如果文件增删达到五次就所有节点重构树
         */
        int ModifyTreeCount = 0;

        while(true) {
            System.out.println("开始同步");
            //同步目录和树，当前目录修改则修改树
            if(compare(dir1,dir2,merkleTree1,merkleTree2))
            {
                ModifyTreeCount++;
                System.out.println("同步目录 "+ dir2);
            }
            if(compare(dir1,dir3,merkleTree1,merkleTree3))
            {
                ModifyTreeCount++;
                System.out.println("同步目录 "+ dir3);
            }
            if(compare(dir1,dir4,merkleTree1,merkleTree4))
            {
                ModifyTreeCount++;
                System.out.println("同步目录 "+ dir4);
            }

            if(ModifyTreeCount == 2)
            {
                //休眠10s，再次进行循环（如果同步达到五次就所有节点重构树
                System.out.println("重构目录中的树");
                ModifyTreeCount = 0;
                merkleTree1 = builderDir1.Builder();
                merkleTree2 = builderDir2.Builder();
                merkleTree3 = builderDir3.Builder();
                merkleTree4 = builderDir4.Builder();
                System.out.println("重构完成");
            }
            Thread.currentThread().sleep(10000);//10s轮询一次，获取文件目录
        }
    }
}