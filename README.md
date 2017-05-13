# simpleimageloader
a simple image loader for android using lrucache and disklrucache
## features
including disk cache(so you can load cached pictures when offline) and memory cache,and provide sync and async loading for pictures,
when loading picture to memory,simpleimageloader resize it so it's useful to avoid OOM.many thanks to
the book that I take a reference.
## use 
first,add dependency to your gradle:

    compile 'com.distancelin.simpleimageloader:simpleimageloader:1.0.0'
then it is easy to use,you can use asyn loading as follows:

     SimpleImageLoader.getSingleton(mContext).loadBitmapAsync(targetImageView,url);
 
 and also syn loading by:
 
    SimpleImageLoader.getSingleton(mContext).loadBitmapSync(targetImageView,url);

## this is the demo screenshot using simpleimageloader:
<img src="screenshot/Screenshot.png" width="40%" />
## about me
A sincerer android learner,you can contact me at  
coder_jason@163.com    
QQ:1627642910  
if you find any problems in simpleimageloader,please let me konw it.  
looking forward to your pr,and by the way,may I have your star please 0.0
