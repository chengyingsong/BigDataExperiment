package merkletree;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import merkletree.Leaf;
import merkletree.MerkleTree;

/*从目录中读取文件，构建树
1. 读取目录信息，读取文件，把文件名作为block1,文件内容作为block2；
2. 把字符串转成byte数组，并初始化叶节点
3. 两两组合为子节点，（落单就选择前一个子节点构成一个新的子节点）
4. 子节点不断组合，直到只剩下最后一个根节点
5. 返回根节点
*/
public class TreeBuilderDir {

    private final String basePath;


    public TreeBuilderDir(String basePath) {
        this.basePath = basePath;
    }

    //按照目录建立相应的树
    public MerkleTree Builder() throws IOException {

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            // Should never happen, we specified SHA, a valid algorithm
            assert false;
        }

        //获取文件列表
        File[] filelist = new File(this.basePath).listFiles();
        List<Leaf> leafs = new ArrayList<Leaf>();

        for (File file : filelist) {
            //将目录转成byte数组
            String filename = file.getName();
            byte[] block1 = filename.getBytes();
            //打开文件并获取文件内容

            BufferedReader br = new BufferedReader(new FileReader(file.getPath()));
            String content = br.readLine();
            br.close();


            //System.out.println(filename + ":  " + content);

            List<byte[]> blocks = new ArrayList<byte[]>();
            blocks.add(block1);
            byte[] block2 = new byte[0];
            try
            {
                block2=content.getBytes();
                blocks.add(block2);
            }catch (Exception e){
                blocks.add(block2);
                //System.out.println("空文件");
            }
            Leaf leaf = new Leaf(blocks,filename);   //初始化叶节点
            leafs.add(leaf);  //使用列表存储叶节点
        }

        List<MerkleTree> branchs = new ArrayList<MerkleTree>();
        for (int i = 0; i < leafs.size(); i += 2) {
            //System.out.println(leafs.get(i).toString());
            //叶节点两两组合为树节点
            if (i + 1 != leafs.size()) {
                //System.out.println(leafs.get(i+1).toString());
                MerkleTree branch = new MerkleTree(md);
                branch.add(leafs.get(i), leafs.get(i + 1));
                leafs.get(i).setFather(branch);
                leafs.get(i+1).setFather(branch);

                branch.addfiles(leafs.get(i).getFilename());
                branch.addfiles(leafs.get(i+1).getFilename());
                branchs.add(branch);
            } else {
                //System.out.println("!!!");
                MerkleTree branch = new MerkleTree(md);
                branch.add(branchs.get(branchs.size() - 1), leafs.get(i));

                branchs.get(branchs.size()-1).setFather(branch);
                leafs.get(i).setFather(branch);

                branch.addfiles(branchs.get(branchs.size()-1).getFilelists());
                branch.addfiles(leafs.get(i).getFilename());

                branchs.remove(branchs.size()-1);
                branchs.add(branch);
            }
        }


        //用i来覆盖写入，用j来+2组合
        while(branchs.size() != 1)
        {
             int n = branchs.size();

            //枝叶节点两两组合为父节点
            for (int j = 0; j < n; j += 2) {
                if (j + 1 != n) {
                    MerkleTree branch = new MerkleTree(md);
                    branch.add(branchs.get(j), branchs.get(j + 1));
                    branchs.get(j).setFather(branch);
                    branchs.get(j+1).setFather(branch);
                    branch.addfiles(branchs.get(j).getFilelists());
                    branch.addfiles(branchs.get(j+1).getFilelists());
                    branchs.add(branch);
                }
                else{
                    MerkleTree branch = new MerkleTree(md);
                    branch.add(branchs.get(branchs.size()-1),branchs.get(j));
                    branchs.get(branchs.size()-1).setFather(branch);
                    branchs.get(j).setFather(branch);
                    branch.addfiles(branchs.get(j).getFilelists());
                    branch.addfiles(branchs.get(branchs.size()-1).getFilelists());
                    branchs.remove(branchs.size()-1);
                    branchs.add(branch);
                }
            }
            //System.out.println(branchs.size());
            //System.out.println(n);
            //不记录上一层的信息
            for(int j=0;j<n;j++)
                 branchs.remove(0);

        }
        //branchs.get(0).prettyPrint();
        //最终列表中只有根节点
        return branchs.get(0);

    }
}
