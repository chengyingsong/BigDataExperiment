package merkletree;

import java.io.*;

public class ModifyDir {

    //按照返回的message要求对目录进行相应的增删改
    public   static  void Modify(String basedir,String modifydir,String modifyfilename) throws IOException {
        //在主目录中找到原文件
        File[] filelist = new File(basedir).listFiles();
        File basefile = null;
        //System.out.println(modifyfilename);

        for(File file:filelist)
        {
            //System.out.println(file.getName());
            if(file.getName().equals(modifyfilename)) {
                basefile = file;
                break;
            }
        }
        //在从目录中找到要修改的文件
        File[] filelist2 = new File(modifydir).listFiles();
        File modifyfile = null;
        for(File file:filelist2)
        {
            if(file.getName().equals(modifyfilename))
            {
                modifyfile = file;
                break;
            }
        }

        //获取原文件类容
        FileReader fr = new FileReader(basefile);
        BufferedReader br = new BufferedReader(fr);
        String basecontent = br.readLine();
        System.out.println(modifyfilename+"文件的原本内容为"+basecontent);
        fr.close();

        FileReader fr2 = new FileReader(modifyfile);
        BufferedReader br2 = new BufferedReader(fr2);
        String modifycontent = br2.readLine();
        System.out.println(modifyfilename+"文件现在的内容为"+modifycontent);
        fr2.close();
        //把内容写入要修改的文件中
        try{
            FileWriter fw = new FileWriter(modifyfile);
            fw.write(basecontent);
            fw.flush();
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        System.out.println("Successful Modify in dir!!");
    }



    public static void Add(String basedir,String adddir,String addfilename)
    {
        //从basedir中找到文件，添加到adddir中
        File[] filelist = new File(basedir).listFiles();
        File basefile = null;
        //System.out.println(addfilename);
        for(File file:filelist)
        {
            //System.out.println(file.getName());
            if(file.getName().equals(addfilename)) {
                basefile = file;
                break;
            }
        }
        int bytesum = 0;
        int byteread = 0;
        try{
            if(basefile.exists())
            {
                FileInputStream inStream = new FileInputStream(basefile);
                FileOutputStream fs = new FileOutputStream(new File(adddir,addfilename));
                byte[] buffer =new byte[5120];
                int length;
                while((byteread = inStream.read(buffer)) != -1){
                    bytesum += byteread;
                    //System.out.println((bytesum));
                    fs.write(buffer,0,byteread);
                }
                inStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Successful Add in dir!!");
    }


    public static void delete(String deletedir,String deletefilename) throws IOException {
        //在从目录中找到要删除的文件
        File[] filelist = new File(deletedir).listFiles();
        File deletefile = null;
        for(File file:filelist)
        {
            if(file.getName().equals(deletefilename))
            {
                deletefile = file;
                break;
            }
        }

        boolean result =false;
        int trycount =0;
        while(!result && trycount++ < 10){
            System.gc();
            try{
                result = deletefile.delete();
                if(result)
                    System.out.println("Successful Delete in dir!!");
            }catch (Exception e)
            {
                System.out.println("删除文件操作出错");
                e.printStackTrace();
            }
        }

    }

}
