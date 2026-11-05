package com.knowledge.app.service.document;
import java.util.*;
import com.knowledge.app.model.entity.Document;
import com.knowledge.app.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.*;
@Service public class DocumentSearch {
 private final JdbcTemplate jdbc;private final DocumentRepository docs;private final boolean mysql;
 public DocumentSearch(JdbcTemplate jdbc,DocumentRepository docs,javax.sql.DataSource source)throws java.sql.SQLException{this.jdbc=jdbc;this.docs=docs;try(var connection=source.getConnection()){mysql=connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL");}}
 public Page<Document> search(String q,Long category,String access,Long school,Long faculty,Long subject,String tag,String order,int page,int size){
  StringBuilder where=new StringBuilder(" FROM documents d JOIN categories c ON c.id=d.category_id WHERE d.status='PUBLISHED' AND c.active=true");var args=new ArrayList<Object>();
  for(var filter:List.of(new Filter("d.category_id",category),new Filter("d.institution_id",school),new Filter("d.faculty_id",faculty),new Filter("d.subject_id",subject)))if(filter.value()!=null){where.append(" AND ").append(filter.column()).append("=?");args.add(filter.value());}
  if(!access.isBlank()){where.append(" AND d.access_type=?");args.add(access);}
  if(!q.isBlank()){where.append(" AND (");if(mysql){where.append("MATCH(d.title,d.summary,d.description) AGAINST (? IN NATURAL LANGUAGE MODE)>0 OR ");args.add(q);}where.append("LOWER(d.title) LIKE ? ESCAPE '!' OR LOWER(d.summary) LIKE ? ESCAPE '!' OR LOWER(d.description) LIKE ? ESCAPE '!' OR EXISTS (SELECT 1 FROM document_tags dt JOIN tags t ON t.id=dt.tag_id WHERE dt.document_id=d.id AND LOWER(t.name) LIKE ? ESCAPE '!'))");String pattern="%"+escape(q.toLowerCase(Locale.ROOT))+"%";for(int i=0;i<4;i++)args.add(pattern);}
  if(tag!=null&&!tag.isBlank()){where.append(" AND EXISTS(SELECT 1 FROM document_tags dt JOIN tags t ON t.id=dt.tag_id WHERE dt.document_id=d.id AND LOWER(t.name)=?)");args.add(tag.trim().toLowerCase(Locale.ROOT));}
  Long total=jdbc.queryForObject("SELECT COUNT(*)"+where,Long.class,args.toArray());
  String sort=switch(order){case "title"->"d.title ASC,d.id DESC";case "featured"->"d.featured DESC,d.published_at DESC,d.id DESC";case "popular"->"(SELECT COUNT(*) FROM document_downloads dl WHERE dl.document_id=d.id AND dl.created_at>=?) DESC,d.id DESC";default->"d.published_at DESC,d.id DESC";};
  if(order.equals("popular"))args.add(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(7)));
  args.add(size);args.add((long)page*size);var ids=jdbc.query("SELECT d.id"+where+" ORDER BY "+sort+" LIMIT ? OFFSET ?",(row,n)->row.getLong(1),args.toArray());
  var byId=new HashMap<Long,Document>();docs.findAllById(ids).forEach(d->byId.put(d.getId(),d));return new PageImpl<>(ids.stream().map(byId::get).toList(),PageRequest.of(page,size),total==null?0:total);
 }
 private String escape(String s){return s.replace("!","!!").replace("%","!%").replace("_","!_");}
 private record Filter(String column,Long value){}
}
