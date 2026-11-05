import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
public class CleanupBrowserTestUsers {
 public static void main(String[] args)throws Exception {
  var names=new LinkedHashSet<String>();
  for(String manifest:List.of("e2e-users.json","e2e-library-users.json","e2e-organization-users.json","e2e-recovery-users.json","e2e-sprint-users.json","e2e-sprint2-users.json")){
   Path path=Path.of("config",manifest);if(!Files.exists(path))continue;
   var matcher=Pattern.compile("\"(e2e_(?:managed|member|library|organization|sprint|expert|s2author|s2admin)_[a-z0-9]+)\"").matcher(Files.readString(path));while(matcher.find())names.add(matcher.group(1));
  }
  if(names.size()>20)throw new IllegalStateException("Unexpected test manifest size");
  var p=new Properties();try(var reader=Files.newBufferedReader(Path.of("config/application-dev.properties"))){p.load(reader);}
  var keys=new ArrayList<String>();var avatarKeys=new ArrayList<String>();
  try(var c=DriverManager.getConnection("jdbc:mysql://localhost:3306/knowledge_hub",p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
   c.setAutoCommit(false);int removed=0;
   for(String name:names)try(var q=c.prepareStatement("SELECT id FROM users WHERE username=? AND email=?")){
    q.setString(1,name);q.setString(2,name+"@example.test");try(var r=q.executeQuery()){if(!r.next())continue;long id=r.getLong(1);var docs=new ArrayList<Long>();
     try(var s=c.prepareStatement("SELECT id,title FROM documents WHERE author_id=?")){s.setLong(1,id);try(var rows=s.executeQuery()){while(rows.next()){if(!rows.getString(2).startsWith("E2E organization ")&&!rows.getString(2).startsWith("E2E Sprint2 "))throw new IllegalStateException("Unexpected authored document; cleanup stopped");docs.add(rows.getLong(1));}}}
     try(var s=c.prepareStatement("SELECT avatar_url FROM user_profiles WHERE user_id=?")){s.setLong(1,id);try(var rows=s.executeQuery()){while(rows.next()){String url=rows.getString(1);if(url!=null&&url.matches("/api/avatars/[a-f0-9-]{36}[.]png"))avatarKeys.add(url.substring(url.lastIndexOf('/')+1));}}}
     try(var d=c.prepareStatement("DELETE FROM user_follows WHERE follower_id=? OR following_id=?")){d.setLong(1,id);d.setLong(2,id);d.executeUpdate();}
     for(String table:List.of("document_downloads","document_bookmarks","document_access","user_profiles","user_roles"))try(var d=c.prepareStatement("DELETE FROM "+table+" WHERE user_id=?")){d.setLong(1,id);d.executeUpdate();}
     for(Long doc:docs){
      for(String table:List.of("document_downloads","document_bookmarks","document_access"))try(var s=c.prepareStatement("SELECT COUNT(*) FROM "+table+" WHERE document_id=?")){s.setLong(1,doc);try(var rows=s.executeQuery()){rows.next();if(rows.getInt(1)>0)throw new IllegalStateException("Document has other users' data; cleanup stopped");}}
      try(var s=c.prepareStatement("SELECT storage_key FROM document_files WHERE document_id=?")){s.setLong(1,doc);try(var rows=s.executeQuery()){while(rows.next()){String key=rows.getString(1);if(!key.matches("upload-[a-f0-9-]{36}[.](pdf|ppt|pptx|docx|md|txt)"))throw new IllegalStateException("Unexpected test storage key");keys.add(key);}}}
      try(var d=c.prepareStatement("DELETE FROM document_tags WHERE document_id=?")){d.setLong(1,doc);d.executeUpdate();}
      try(var d=c.prepareStatement("DELETE FROM document_files WHERE document_id=?")){d.setLong(1,doc);d.executeUpdate();}
      try(var d=c.prepareStatement("DELETE FROM documents WHERE id=? AND author_id=?")){d.setLong(1,doc);d.setLong(2,id);d.executeUpdate();}
     }
     try(var d=c.prepareStatement("DELETE FROM users WHERE id=? AND username=? AND email=?")){d.setLong(1,id);d.setString(2,name);d.setString(3,name+"@example.test");removed+=d.executeUpdate();}
    }
   }
   for(String name:names)if(name.startsWith("e2e_s2author_")){String tag="E2E-"+name.substring("e2e_s2author_".length());try(var d=c.prepareStatement("DELETE FROM tags WHERE name=? AND NOT EXISTS(SELECT 1 FROM document_tags dt WHERE dt.tag_id=tags.id) AND NOT EXISTS(SELECT 1 FROM post_tags pt WHERE pt.tag_id=tags.id)")){d.setString(1,tag);d.executeUpdate();}}
   c.commit();System.out.println("Removed disposable browser-test accounts: "+removed);
  }
  Path avatarDirectory=Path.of("config/avatar-storage").toAbsolutePath().normalize();
  for(String key:avatarKeys){Path file=avatarDirectory.resolve(key).normalize();if(!file.startsWith(avatarDirectory)||Files.isSymbolicLink(file)||Files.isSymbolicLink(avatarDirectory))throw new IllegalStateException("Unsafe avatar fixture path");Files.deleteIfExists(file);}
  Path directory=Path.of("config/document-storage").toAbsolutePath().normalize();
  for(String key:keys){Path file=directory.resolve(key).normalize();if(!file.startsWith(directory)||Files.isSymbolicLink(file)||Files.isSymbolicLink(directory))throw new IllegalStateException("Unsafe fixture file path");Files.deleteIfExists(file);}
 }
}