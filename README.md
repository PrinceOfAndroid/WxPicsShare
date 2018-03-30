# WxPicsShare
微信多图+文字分享

## 核心代码
``` java
Intent intent = new Intent();
//intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI"));  分享给好友
intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI"));
intent.putExtra("Kdescription", "分享测试");
intent.setAction(Intent.ACTION_SEND_MULTIPLE);
intent.setType("image/*");
ArrayList<Uri> imageUris = new ArrayList<>();
for (File f : imgFile) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        imageUris.add(getImageContentUri(MainActivity.this, f));
    } else {
        imageUris.add(Uri.fromFile(f));
    }
}
intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
startActivity(intent);
```

## 踩坑
安卓7.0后不再允许在app中把file://Uri暴露给其他app,也就是说不能使用Uri.fromFile(f)，于是使用FileProvider方案生成uri:
``` java
// 将文件转换成content://Uri的形式
    Uri photoURI = FileProvider.getUriForFile(activity,
            activity.getPackageName()+ ".provider",
            new File(photoPath));
```
但获取的uri并不能分享...也许微信无法识别，那换另一种方案，开启严苛模式：
``` java
 //解决android N（>=24）系统以上分享 路径为file://时的 android.os.FileUriExposedException异常
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
```
虽然可以成功进行分享，可严苛模式只建议在debug下开启，release是很危险的，因为如果代码有小问题，此模式会闪退...

## 百度后最终方案
``` java
 /**
     * 获取图片的绝对的分享地址（待研究）
     *
     * @param context
     * @param imageFile
     * @return content Uri
     */
    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }
```
