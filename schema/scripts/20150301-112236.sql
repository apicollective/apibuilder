update versions
   set version_sort_key = version_sort_key || '|9'
 where version_sort_key not like '%|%';
