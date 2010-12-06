<?php
function cleanRDF($pageContents){
$pageContents = str_replace("rdf:RDF", "rdf_RDF", $pageContents);
$pageContents = str_replace("rdf:type", "rdf_type", $pageContents);
$pageContents = str_replace("rdf:RDF", "rdf_RDF", $pageContents);
$pageContents = str_replace("rdf:Description", "rdf_Description", $pageContents);
$pageContents = str_replace("rdf:resource", "rdf_resource", $pageContents);
$pageContents = str_replace("rdfs:label", "rdfs_label", $pageContents);
$pageContents = str_replace("rdf:about", "rdf_about", $pageContents);
$pageContents = str_replace("j.0:", "", $pageContents);
$pageContents = str_replace("j.1:", "", $pageContents);
$pageContents = str_replace("j.2:", "", $pageContents);
$pageContents = str_replace("j.3:", "", $pageContents);
$pageContents = str_replace("j.4:", "", $pageContents);
$pageContents = str_replace("j.5:", "", $pageContents);
$pageContents = str_replace("j.6:", "", $pageContents);
return $pageContents;
} 
?>
<?php 
function echoDiv($id, $string)
{
	if (isset($string))
	{
	echo "<div id=\"".$id."\">".$string."</div>\n";
	}
}
?>
<?php 
function getURI($URL)
{
	$URL = str_replace("http://", "", $URL);
	$URL = str_replace("https://", "", $URL);
	$URLData = explode("/", $URL);
	$URLLength = sizeof($URLData);
	$URLLength = $URLLength - 1;
	return $URLData[$URLLength];
	
}
function getSite($URL)
{
	$URL = str_replace("http://", "", $URL);
	$URL = str_replace("https://", "", $URL);
	$URLData = explode("/", $URL);
	return $URLData[0];
}
?>
<?php
function getVIVOPersonData($search){
//What URI did we search for
$identifier = getURI($search);
$personURI = $search; //What's the VIVO URI for this person?
$site = getSite($search);
//$site = "temp.edu";
$personRDF = "http://".$site."/individual/".$identifier."/".$identifier.".rdf"; //Where is the raw RDF?
//Get the RDF or DIE.
$pageContents = @file_get_contents($personRDF) or die();
//Clean it up so simpleXML will eat it
$pageContents = cleanRDF($pageContents);
//Now let's eat that XML
$xml = simplexml_load_string($pageContents);
//And get the values we want
//Titles need some special handling, if we don't have a preferred title set, go to the position and get that title.
$prefTitle = $xml->xpath("//preferredTitle");
if (!isset($prefTitle[0]))
{
	$position = $xml->xpath("//personInPosition");
	$positionURI = $position[0]['rdf_resource'];
	$positionURI = str_replace("http://".$site."/individual/", "", $positionURI);
	$positionURL = "http://".$site."/individual/".$positionURI."/".$positionURI.".rdf";
	$positionContents = @file_get_contents($positionURL);
	if(strlen($positionContents) > 1){
		$positionContents = cleanRDF($positionContents);
		$positionxml = simplexml_load_string($positionContents);
		$prefTitle = $positionxml->xpath("//hrJobTitle");
		if(!isset($prefTitle[0]))
		{
			$prefTitle = $positionxml->xpath("//rdfs_label");
		}	
		
	}

}

//First names need some special handling as well.
//We want to check for an active directory name first, THEN fallback to rdfs:label.
$name = $xml->xpath("//activeDirName");
if (!isset($name[0]))
{
	$name = $xml->xpath("//rdfs_label");
}

//Images require a bit of handling because they aren't direct links
//First, let's get the full size image (We're doing some of the work for the thumbnail here too
$image = $xml->xpath("//mainImage");
//1.1: Make sure they have an image.
if (isset($image[0])) {
$imageURI = $image[0]['rdf_resource'];
$imageURI = str_replace("http://".$site."/individual/", "", $imageURI);
$imageURL = "http://".$site."/individual/".$imageURI."/".$imageURI.".rdf";
$imagedata = @file_get_contents($imageURL);
if(strlen($imagedata) > 1){
	$imagedata = cleanRDF($imagedata);
	$imagexml = simplexml_load_string($imagedata);
	$fullsizeURI = $imagexml->xpath("//downloadLocation");
	$fullsizeURI = $fullsizeURI[0]['rdf_resource'];
	$filename = $imagexml->xpath("//filename");
	$fullsizeURI = str_replace("http://vivo.ufl.edu/individual/", "", $fullsizeURI);
	$fullsizeURL = "https://".$site."/file/".$fullsizeURI."/".$filename[0];
}

	
}

//And now let's get the thumbnail
//Commented out, we don't need this right now.
//$thumbURI = $imagexml->xpath("//thumbnailImage");
//$thumbURI = $thumbURI[0]['rdf_resource'];
//$thumbURI = str_replace("http://vivo.ufl.edu/individual/", "", $thumbURI);
//$thumbURL = "https://vivo.ufl.edu/individual/".$thumbURI."/".$thumbURI.".rdf";
//$thumbentdata = file_get_contents($thumbURL);
//$thumbentdata = cleanRDF($thumbentdata);
//$thumbentxml = simplexml_load_string($thumbentdata);
//$thumbfilename = $thumbentxml->xpath("//filename");
//$thumbdownload = $thumbentxml->xpath("//downloadLocation");
//$thumbdownload = $thumbdownload[0]['rdf_resource'];
//$thumbdownload = str_replace("http://vivo.ufl.edu/individual/", "", $thumbdownload);
//$thumbdownloadURL = "https://vivo.ufl.edu/file/".$thumbdownload."/".$thumbfilename[0];

//And now everything else
$email = $xml->xpath("//workEmail");
$phone = $xml->xpath("//workPhone");
//We were never told to get overview, but I'm leaving code in place in case we want it in the future.
//$overview = $xml->xpath("//overview");
$fax = $xml->xpath("//workFax");
//and assign them to easier variables (so lazy)
$vivoName = strip_tags($name[0]);
//Uuugly hack.
if (strlen($vivoName) > 50)
{
	$vivoName = strip_tags($name[1]);
}
$vivoEmail = strip_tags($email[0]);
$vivoPhone = strip_tags($phone[0]);
$vivoFax = strip_tags($fax[0]);
$vivoTitle = strip_tags($prefTitle[0]);
$vivoImage = strip_tags($fullsizeURL);
//$vivoImage = "<img src=\"".$vivoImage."\">";
$vivoLink = strip_tags($personURI);

//Now let's output the data.
echoDiv("vivoName", $vivoName);
echoDiv("vivoEmail", $vivoEmail);
echoDiv("vivoPhone", $vivoPhone);
echoDiv("vivoFax", $vivoFax);
echoDiv("vivoTitle", $vivoTitle);
echoDiv("vivoImage", $vivoImage);
echoDiv("vivoLink", $vivoLink);


}
?>