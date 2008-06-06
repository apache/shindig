<?php
require_once "../PHPUnit/Framework/TestSuite.php";
require_once "../PHPUnit/TextUI/TestRunner.php";

// JUST FOR DEV

function readDirR($dirName, &$names){
	if(is_file($dir)){
				return;
	}
	$dir = opendir($dirName);
	if(count($dir)> 2 ){
		foreach ($dir as $file){
			if($file != '..' && $file != '.'){
				if(is_dir($file)){
					readDirR($dirName.'/'.$file, $names);
				}
				$clase = str_replace('.php','',$file);
				$names[] = array('path' => $dirName.'/'.$file,'name' => $clase);
			}
		}
	}

}

$clases = array();
readDirR('.', $clases);
$suite  = new PHPUnit_Framework_TestSuite();

foreach($clases as $clase){
	if(file_exists($clase['path'])){
		@require_once $clase['path'];
		if(class_exists($clase['name'])){
			$suite->addTestSuite($clase['name']);
		}
	}
}

$result = new PHPUnit_TextUI_TestRunner();
echo '<html><head><title>TestCases</title></head><body><h1>Test Cases</h1><pre>'; 
	$result->doRun($suite);
echo'</pre></body></html>'
?>