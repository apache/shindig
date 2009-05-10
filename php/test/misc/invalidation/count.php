<?php
@date_default_timezone_set(@date_default_timezone_get());

$filename = '/tmp/shindig_test_misc_invalidation_count';

if (file_exists($filename)) {
  $count = file_get_contents($filename);
} else {
  touch($filename);
  $count = 0;
}

$count += 1;
echo "Count: $count at time: " . date('Y-m-d H:i:s');

file_put_contents($filename, $count);