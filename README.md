# BigDataExperiment
大数据实验（暂时是实验3

## 实验3

Windows 下四个文件夹的监控同步和调度，三个从目录跟从主目录的改变。

主要思路是：
1. 在四个文件夹中由目录建树，建树过程中为了之后方便可以设置father字段和文件名列表字段。
2. 将三棵树和主树进行比较，定位不同点然后返回信息
3. 根据信息，主目录和从目录同步
4. 从目录树和目录同步，即保持树结构尽量不变来修改树
5. 如果树的修改的次数达到指定次数，重构所有节点目录生成树
6. Sleep10s
