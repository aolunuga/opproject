package onepoint.project.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;

public class OpBulkFetchIterator<ELEMENT, ID> implements Iterator<ELEMENT> {
   
   public static interface IdConverter<ID> {
      public long getLongFromId(ID id);
   }

   public static interface QueryPreparator {
      public OpQuery prepareQuery(OpQuery q, Collection<Long> ids);
   }

   public static class LocatorIdConverter implements IdConverter<String> {
      public long getLongFromId(String id) {
         return OpLocator.parseLocator(id).getID();
      }
   }
   
   public static class LongIdConverter implements IdConverter<Long> {
      public long getLongFromId(Long id) {
         return id.longValue();
      }
   }
   
   public static class SimpleQueryPreparator implements QueryPreparator {
      
      private String idsCollName = null;
      
      public SimpleQueryPreparator(String idsCollName) {
         this.idsCollName = idsCollName;
      }
      public OpQuery prepareQuery(OpQuery q, Collection<Long> ids) {
         q.setCollection(idsCollName, ids);
         return q;
      }
   }
   
   public boolean hasNext() {
      bulkFetch();
      return bulkResultIterator != null ? bulkResultIterator.hasNext() : false;
   }

   public ELEMENT next() {
      bulkFetch();
      if (bulkResultIterator != null) {
         return bulkResultIterator.next();
      }
      throw new NoSuchElementException();
   }

   public void remove() {
      throw new UnsupportedOperationException();
   }

   private OpBroker broker = null;
   private Iterator<ID> ids = null;
   private IdConverter<ID> idConverter = null;
   private QueryPreparator queryPreparator = null;
   private OpQuery bulkQuery = null;
   private Iterator<ELEMENT> bulkResultIterator = null;

   public static final int BULK_FETCH_SIZE = 250;
   
   public OpBulkFetchIterator(OpBroker broker, Iterator<ID> ids, OpQuery bulkQuery, IdConverter<ID> idConverter, String idsCollectionName) {
      this(broker, ids, bulkQuery, idConverter, new SimpleQueryPreparator(idsCollectionName));
   }
   
   public OpBulkFetchIterator(OpBroker broker, Iterator<ID> ids, OpQuery bulkQuery, IdConverter<ID> idConverter, QueryPreparator queryPreparator) {
      this.broker = broker;
      this.ids = ids;
      this.idConverter = idConverter;
      this.queryPreparator = queryPreparator;
      this.bulkQuery = bulkQuery;
   }
   
   private void bulkFetch() {
      if (bulkResultIterator != null && bulkResultIterator.hasNext()) {
         return;
      }
      List<Long> tmpIDs =  new ArrayList<Long>();
      while (ids.hasNext() && tmpIDs.size() < BULK_FETCH_SIZE) {
         ID id = ids.next();
         tmpIDs.add(new Long(idConverter.getLongFromId(id)));
      }
      if (tmpIDs.size() > 0) {
         if (queryPreparator != null) {
            bulkQuery = queryPreparator.prepareQuery(bulkQuery, tmpIDs);
         }
         else {
            bulkQuery.setCollection("bulk_ids", tmpIDs);
         }
         bulkResultIterator = broker.list(bulkQuery).iterator();
      }
      else {
         bulkResultIterator = null;
      }
   }

}
