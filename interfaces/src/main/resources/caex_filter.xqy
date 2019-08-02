module namespace caex215 = "http://ipr.kit.edu/caex";
import module namespace functx = "http://www.functx.com" at "./functx-1.0.xqy";
(:import module namespace basics = "http://ipr.kit.edu/xquery/basics" at "/Users/aris/Documents/repositories/ipr/aml_tools/resources/xquery/basics.xqy";:)

(:check if the context node is a library:)
declare function caex215:isLib($context){
  if ($context[self::InterfaceClassLib] or $context[self::RoleClassLib] or $context[self::SystemUnitClassLib] or $context[self::InstanceHierarchy])
  then fn:true()
  else fn:false()
};

(:get the library od the context node:)
declare function caex215:getLib($context){
  $context/ancestor-or-self::*[caex215:isLib(.)]
};

declare function caex215:getLibFromPath($path as xs:string) as xs:string {
  fn:substring-before($path, "/")
};

declare function caex215:getNameFromPath($path as xs:string) as xs:string {
  functx:substring-after-last($path, "/")
};

(:get the full path of this caex class node:)
declare function caex215:getPath($node) as xs:string{
  let $ref := string($node/@RefBaseClassPath)
  let $lib := string(caex215:getLib($node)/@Name)
  let $name := string($node/@Name)
  return
    if(caex215:isLib($node))
    then $node/@Name
    else
      if($ref)
      then
        let $reflib := caex215:getLibFromPath($ref) 
        let $refparent := caex215:getNameFromPath($ref)
        return
          if($reflib = $lib)
          then fn:concat($ref, "/", $name)
          else if ($reflib = string($node/parent::self/@Name))
          then fn:concat(caex215:getPath($node/..), "/", $name)
          else fn:concat(caex215:getPath($node/..), "/", $name)(:fn:concat($lib, "/", $name):)
      else
        fn:concat($lib, "/", $name)
};

(:==================================================================:)
(:Function to get all children RC of an given RC:)
(:==================================================================:)
declare function caex215:getChildRC($refbase as xs:string, $root as node()) as item()*{ 
    (:notice the parameter refbase is the full path of the parent role :)  
    let $plib := caex215:getLibFromPath($refbase)
    let $pname := caex215:getNameFromPath($refbase)
    return
      <children>
      {
        for $candidate in $root//RoleClass
        let $cname := string($candidate/@Name)
        let $clib := string(caex215:getLib($candidate)/@Name)
        let $cref := string($candidate/@RefBaseClassPath)
        return            
            
              if ($cref = $refbase) (:directly specified subclass:)
              then
                <child>
                  <cpath>{caex215:getPath($candidate)}</cpath>
                  <ppath>{$refbase}</ppath>
                </child> 
              else
              if ($root//RoleClassLib[@Name=caex215:getLibFromPath($cref)])
              then ()
              else
              let $xmlparent := $candidate/parent::RoleClass
              return      
                  if($xmlparent) (:if the candidate has no xml parent, ignore it:)
                  then
                      let $path := caex215:getPath($xmlparent)
                      return
                          if($path = $refbase)
                          then
                            <child>
                              <cpath>{caex215:getPath($candidate)}</cpath>
                              <ppath>{$refbase}</ppath>
                            </child> 
                          else ()
                else()
       }
        </children>    
};

(:==================================================================:)
(:Function to get all descendants RC of an given RC:)
(:==================================================================:)
declare function caex215:getDescendantRC($refbase as xs:string, $root as node()) as item()*{
	let $children := caex215:getChildRC($refbase, $root)      
  return  
      for $idx in 1 to count($children/child)        
        let $cpath := $children/child[$idx]/cpath
        let $ppath := $children/child[$idx]/ppath
        let $clib := caex215:getLibFromPath($cpath)
        let $cname := caex215:getNameFromPath($cpath)
        let $node := $root//RoleClass[@Name=$cname and string(caex215:getLib(.)/@Name)=$clib]        
        return (        
          <descendant>{$cpath/text()}</descendant>,
          for $n in $node
          return caex215:getDescendantRC(caex215:getPath($n), $root)
       )
};

(:==================================================================:)
(:Function to get all descendants RC of an given RC:)
(:Reloaded for convenient usage:)
(:==================================================================:)
declare function caex215:getDescendantRC($role as xs:string, $lib as xs:string, $root as node()) as item()* {
  let $node := $root//RoleClass[@Name=$role and caex215:getLib(.)/@Name = $lib]
  return caex215:getDescendantRC(caex215:getPath($node), $root)
};


(:==================================================================:)
(:Function to get all children IC of an given IC:)
(:==================================================================:)
declare function caex215:getChildIC($refbase as xs:string, $root as node()) as item()*{ 
    <children>
    {
      for $candidate in $root//InterfaceClass
      let $cname := string($candidate/@Name)
      let $clib := string(caex215:getLib($candidate)/@Name)
      let $cref := string($candidate/@RefBaseClassPath)
      return            
          
            if ($cref = $refbase) (:directly specified subclass:)
            then
              <child>
                <cpath>{caex215:getPath($candidate)}</cpath>
                <ppath>{$refbase}</ppath>
              </child> 
            else
            if ($root//InterfaceClassLib[@Name=caex215:getLibFromPath($cref)])
            then ()
            else
            let $xmlparent := $candidate/parent::InterfaceClass
            return      
                if($xmlparent) (:if the candidate has no xml parent, ignore it:)
                then
                    let $path := caex215:getPath($xmlparent)
                    return
                        if($path = $refbase)
                        then
                          <child>
                            <cpath>{caex215:getPath($candidate)}</cpath>
                            <ppath>{$refbase}</ppath>
                          </child> 
                        else ()
              else()
     }
    </children>    
};

(:==================================================================:)
(:Function to get all descendants IC of an given IC:)
(:==================================================================:)
declare function caex215:getDescendantIC($refbase as xs:string, $root as node()) as item()*{
	let $children := caex215:getChildIC($refbase, $root)      
  return  
      for $idx in 1 to count($children/child)        
        let $cpath := $children/child[$idx]/cpath
        let $ppath := $children/child[$idx]/ppath
        let $clib := caex215:getLibFromPath($cpath)
        let $cname := caex215:getNameFromPath($cpath)
        let $node := $root//InterfaceClass[@Name=$cname and string(caex215:getLib(.)/@Name)=$clib]        
        return (        
          <descendant>{$cpath/text()}</descendant>,
          for $n in $node
          return caex215:getDescendantIC(caex215:getPath($n), $root)
       )
};

(:==================================================================
:Function to get all descendant IC of an given IC
:Reloaded function for convenient usage
:==================================================================:)
declare function caex215:getDescendantIC($role as xs:string, $lib as xs:string, $root as node()) as item()* {
  let $node := $root//InterfaceClass[@Name=$role and caex215:getLib(.)/@Name = $lib]
  return caex215:getDescendantIC(caex215:getPath($node), $root)
};


(:==================================================================
:Function to  match a value with attributes of one context node
:==================================================================:)
declare function caex215:matchAttribute($attribute_name as xs:string, $value as xs:string, $context as node()){
  fn:exists($context/Attribute[@Name=$attribute_name]/Value[text()=$value])
};

(:==================================================================
:Function to  match a value with attributes of one context node recursively
:==================================================================:)
declare function caex215:matchAttributeRecursive($attribute_name as xs:string, $value as xs:string, $context as node()){
	if (caex215:matchAttribute($attribute_name, $value, $context))
	then fn:true()
	else 
		for $child in $context/Attribute
		return (caex215:matchAttributeRecursive($attribute_name, $value, $child))
};

(:==================================================================
: Function to get the name and id of the interface and its owner 
: from the given refpartnerside string
:==================================================================:)

declare function caex215:parseRefPartnerSide($refPartner as xs:string, $context as node()){
  let $id_token := fn:tokenize($refPartner, ':')[1]
	let $name_token := fn:tokenize($refPartner, ':')[2]
	(:substring starts from the second character, and has a length - 2: 1 for [, 1 for ]:)
	let $obj_id := fn:substring($id_token, 2, fn:string-length($id_token)-2)
	let $interface_name := fn:substring($name_token, 2, fn:string-length($name_token)-2)
	let $obj := root($context)//*[@ID=$obj_id]
  let $interface := $obj/ExternalInterface[@Name=$interface_name]
  return 
    <partner>
      <oname>{string($obj/@Name)}</oname>
      <oid>{$obj_id}</oid>
      <iname>{string($interface/@Name)}</iname>
      <iid>{string($interface/@ID)}</iid>
    </partner>
    (:($obj/@Name, $obj_id, $interface/@Name, $interface/@ID):)
};

declare function caex215:parseRefSide($refSide as xs:string, $context as node()){
  let $id_token := fn:tokenize($refSide, ':')[1]
	let $name_token := fn:tokenize($refSide, ':')[2]
	(:substring starts from the second character, and has a length - 2: 1 for [, 1 for ]:)
	let $obj_id := fn:substring($id_token, 2, fn:string-length($id_token)-2)
	let $interface_name := fn:substring($name_token, 2, fn:string-length($name_token)-2)
	let $obj := root($context)//*[@ID=$obj_id]
  let $interface := $obj/ExternalInterface[@Name=$interface_name]
  return 
    <refSide>
      <oname>{string($obj/@Name)}</oname>
      <oid>{$obj_id}</oid>
      <iname>{string($interface/@Name)}</iname>
      <iid>{string($interface/@ID)}</iid>
    </refSide>
    (:($obj/@Name, $obj_id, $interface/@Name, $interface/@ID):)
};

declare function caex215:test($id as xs:string){
  $id
};

(:==================================================================
: Function to get all interfaces which are connected to 
: the given interface via InternalLink
: ==================================================================:)
declare function caex215:getConnectedInterfaces($interface as node()){
  if (not($interface[self::ExternalInterface]))
  then ()
  else
    let $links := root($interface)//InternalLink
    let $iname := $interface/@Name
    let $oid := $interface/parent::*/@ID
    return
      for $link in $links
        return 
          (:we rule out empty oid, thus not covering interfaces of SUCs:)
          if ($oid!="" and contains($link/@RefPartnerSideA, $oid) and contains($link/@RefPartnerSideA, $iname))
          then
            let $self := caex215:parseRefSide($link/@RefPartnerSideA, $interface)
            let $partner := caex215:parseRefSide($link/@RefPartnerSideB, $interface)
            return
              <link>
                <lname>{string($link/@Name)}</lname>
                <self>                
                  <oname>{string($self/oname)}</oname>
                  <oid>{string($self/oid)}</oid>
                  <iname>{string($self/iname)}</iname>
                  <iid>{string($self/iid)}</iid>
                </self>
                <partner>                
                  <oname>{string($partner/oname)}</oname>
                  <oid>{string($partner/oid)}</oid>
                  <iname>{string($partner/iname)}</iname>
                  <iid>{string($partner/iid)}</iid>
                </partner>
              </link>
          (:we rule out empty oid, thus not covering interfaces of SUCs:)
          else if ($oid!="" and contains($link/@RefPartnerSideB, $oid) and contains($link/@RefPartnerSideB, $iname))
          then
            let $self := caex215:parseRefSide($link/@RefPartnerSideB, $interface)
            let $partner := caex215:parseRefSide($link/@RefPartnerSideA, $interface)
            return
              <link>
                <lname>{string($link/@Name)}</lname>
                <self>                
                  <oname>{string($self/oname)}</oname>
                  <oid>{string($self/oid)}</oid>
                  <iname>{string($self/iname)}</iname>
                  <iid>{string($self/iid)}</iid>
                </self>
                <partner>                
                  <oname>{string($partner/oname)}</oname>
                  <oid>{string($partner/oid)}</oid>
                  <iname>{string($partner/iname)}</iname>
                  <iid>{string($partner/iid)}</iid>
                </partner>
              </link>
          else 
            ()
};

(:
declare function caex215:getConnectedInterfaces($interface as node()){
  if (not($interface[self::ExternalInterface]))
  then ()
  else
    let $links := root($interface)//InternalLink
    let $iname := $interface/@name
    let $oids := $interface/parent::*/@ID
    return
      for $link in $links
        for $oid in $oids
        return 
          if (contains($link/@RefPartnerSideA, $oid) and contains($link/@RefPartnerSideA, $iname))
          then
            let $self := caex215:parseRefPartnerSide($link/@RefPartnerSideA, $interface)
            let $partner := caex215:parseRefPartnerSide($link/@RefPartnerSideB, root($interface))
            return
              <link>
                <lname>{string($link/@Name)}</lname>
                <self>                
                  <oname>{string($self/oname)}</oname>
                  <oid>{string($self/oid)}</oid>
                  <iname>{string($self/iname)}</iname>
                  <iid>{string($self/iid)}</iid>
                </self>
                {$partner}
              </link>
          else if (contains($link/@RefPartnerSideB, $oid) and contains($link/@RefPartnerSideB, $iname))
          then
            let $self := caex215:parseRefPartnerSide($link/@RefPartnerSideB, $interface)
            let $partner := caex215:parseRefPartnerSide($link/@RefPartnerSideA, root($interface))
            return
              <link>
                <lname>{string($link/@Name)}</lname>
                <self>                
                  <oname>{string($self/oname)}</oname>
                  <oid>{string($self/oid)}</oid>
                  <iname>{string($self/iname)}</iname>
                  <iid>{string($self/iid)}</iid>
                </self>
                {$partner}
              </link>
          else 
            ()        
};
:)
(:==================================================================
: Function to get all interfaces which are connected to 
: any interfaces of the given object via InternalLink
: ==================================================================:)
declare function caex215:getConnectedInterfacesFromObj($node as node()){
  for $interface in $node/descendant-or-self::ExternalInterface
  return 
    <links>
      {caex215:getConnectedInterfaces($interface)}
    </links>
};

(:==================================================================
: Function to get all the refpartners of the given node
: by checking the internal links of all interfaces of this node
: ==================================================================:)
declare function caex215:getRefPartners($node) as xs:string*{	
	for $id in $node/descendant-or-self::*/@ID
	let $links := root($node)//InternalLink
	return			
		for $link in $links
		return (
			if (contains($link/@RefPartnerSideA, $id))
			then
				$link/@RefPartnerSideB
			else if (contains($link/@RefPartnerSideB, $id))
			then 
				$link/@RefPartnerSideA
			else 
				()
		)
};

(:==================================================================:)
(:Function to check whether one node is connected with any other nodes via InternalLink:)
(:==================================================================:)
declare function caex215:isConnected($node as node()) {
	count(caex215:getRefPartners($node)) > 0
};

declare function caex215:isInterfaceConnectedById($nodeId as xs:string, $context as node()){
  let $node := root($context)//*[@ID=$nodeId]
  return count(caex215:getConnectedInterfaces($node))>0
};

declare function caex215:getConnectedInterfacesById($interfaceId as xs:string, $context as node()){
  let $interface := root($context)//*[@ID=$interfaceId]
  return
    if ($interface)
    then
      let $partnerId := caex215:getConnectedInterfaces($interface)/partner/iid
      return root($context)//*[@ID=$partnerId]
    else
      ()
};

declare function caex215:getConnectedInterfacesByRefpartnerside($refpartner as xs:string, $context as node()){
  let $interfaceId := caex215:parseRefPartnerSide($refpartner, $context)/iid
  return caex215:getConnectedInterfacesById($interfaceId, $context)
};

declare function caex215:isInterfaceConnectedToPartner($interfaceId as xs:string, $partnerId as xs:string, $context as node()){
  let $interface := root($context)//*[@ID=$interfaceId]
  return
    if ($interface)
    then 
      functx:is-node-in-sequence($interface, caex215:getConnectedInterfacesById($partnerId, $context))
    else
      fn:false()
};

declare function caex215:isInterfaceConnectedToPartnerByRefSide($interfaceId as xs:string, $partnerRefSide as xs:string, $context as node()){
  let $interface := root($context)//*[@ID=$interfaceId]
  return
    if ($interface)
    then 
      let $pid := caex215:parseRefSide($partnerRefSide, $context)/iid
      return functx:is-node-in-sequence($interface, caex215:getConnectedInterfacesById($pid, $context))
    else
      fn:false()
};


(:==================================================================:)
(:Function to check whether one node is connected with the partner nodes via InternalLink:)
(:==================================================================:)
declare function caex215:isConnectedWith($node as node(), $partner as node()){
  let $links := (caex215:getConnectedInterfacesFromObj($node))
  return
    count($links[link/partner/oname=$partner/@Name] 
      or $links[link/partner/oid=$partner/@ID]
      or $links[link/partner/iname=$partner/@Name]
      or $links[link/partner/iid=$partner/@ID])>0
};

declare function caex215:isConnectedWithById($nodeId as xs:string, $partnerId as xs:string, $context as node()){
  let $node := root($context)//ExternalInterface[@ID=$nodeId]
  let $partner := root($context)//ExternalInterface[@ID=$partnerId]
  return caex215:isConnectedWith($node, $partner)
};

(:==================================================================:)
(:get the transitive closure of the pattern:)
(:==================================================================:)
(:declare function caex215:getClosure0($node){
	if(caex215:match($node))
	then (
		$node, 
		for $child in $node/*
		return caex215:getClosure0($child)
	)
	else ()
};:)

declare function caex215:getClosure($context as node(), $test as function(node()) as xs:boolean) as node()*{
  if ($test($context))
  then (
		$context, 
		for $child in $context/*
		return caex215:getClosure($child, $test)
	)
	else ()
};

declare function caex215:getIEOfRR($role, $lib, $context) {
  let $rcs := caex215:getDescendantRC($role, $lib, root($context))
  for $node in $context/descendant-or-self::*
  return (	
    if($node/RoleRequirements[fn:starts-with(@RefBaseRoleClassPath, $lib) and fn:ends-with(@RefBaseRoleClassPath, $role)])
    then $node
    else 
      for $idx in 1 to count($rcs)
      let $cname := caex215:getNameFromPath($rcs[$idx])
      let $lname := caex215:getLibFromPath($rcs[$idx])
      return
        if ($node/RoleRequirements[fn:starts-with(@RefBaseRoleClassPath, $lname) and fn:ends-with(@RefBaseRoleClassPath, $cname)])
        then $node
        else ()
  )
};


declare function caex215:getObjsOfSRC($role, $lib, $context) {
  let $rcs := caex215:getDescendantRC($role, $lib, root($context))
  for $node in $context/descendant-or-self::*
  return (	
    if($node/SupportedRoleClass[fn:starts-with(@RefRoleClassPath, $lib) and fn:ends-with(@RefRoleClassPath, $role)])
    then $node
    else 
      for $idx in 1 to count($rcs)
      let $cname := caex215:getNameFromPath($rcs[$idx])
      let $lname := caex215:getLibFromPath($rcs[$idx])     
      return                
        if ($node/SupportedRoleClass[fn:starts-with(@RefRoleClassPath, $lname) and fn:ends-with(@RefRoleClassPath, $cname)])
        then $node
        else ()
  )
};

declare function caex215:supportsRCSimple($role, $lib, $node){
  if($node/SupportedRoleClass[fn:starts-with(@RefRoleClassPath, $lib) and fn:ends-with(@RefRoleClassPath, $role)])
  then fn:true()
  else fn:false()
};

(:whether or not the given node supports the role class from the lib:)
declare function caex215:supportsRCRecursive($role, $lib, $node) {
  if (caex215:supportsRCSimple($role, $lib, $node))
  then fn:true()
  else (
    let $rcs := caex215:getDescendantRC($role, $lib, root($node)/*)
    for $idx in 1 to count($rcs)
    let $cname := caex215:getNameFromPath($rcs[$idx])
    let $lname := caex215:getLibFromPath($rcs[$idx])
    return caex215:supportsRCSimple($cname, $lname, $node)
  )
};

declare function caex215:supportsRC($role, $lib, $node){
  if (functx:is-value-in-sequence(fn:true(), caex215:supportsRCRecursive($role, $lib, $node)))
  then fn:true()
  else fn:false()
};

declare function caex215:requiresRRSimple($role, $lib, $node){
  (:if($node/RoleRequirements[fn:starts-with(@RefBaseRoleClassPath, $lib) and fn:ends-with(@RefBaseRoleClassPath, $role)]):)
  (:assume the node is the RR itself:)
  if($node[fn:starts-with(@RefBaseRoleClassPath, $lib) and fn:ends-with(@RefBaseRoleClassPath, $role)])
  then fn:true()
  else fn:false()
};

(:whether or not the given node supports the role class from the lib:)
declare function caex215:requiresRRRecursive($role, $lib, $node) {
  if (caex215:requiresRRSimple($role, $lib, $node))
  then fn:true()
  else (
    let $rcs := caex215:getDescendantRC($role, $lib, root($node)/*)
    for $idx in 1 to count($rcs)
    let $cname := caex215:getNameFromPath($rcs[$idx])
    let $lname := caex215:getLibFromPath($rcs[$idx])
    return caex215:requiresRRSimple($cname, $lname, $node)
  )
};

declare function caex215:requiresRR($role, $lib, $node){
  if (functx:is-value-in-sequence(fn:true(), caex215:requiresRRRecursive($role, $lib, $node)))
  then fn:true()
  else fn:false()
};

declare function caex215:refsICSimple($ic, $lib, $node){
  if($node[fn:starts-with(@RefBaseClassPath, $lib) and fn:ends-with(@RefBaseClassPath, $ic)])
  then fn:true()
  else fn:false()
};

declare function caex215:refsICRecursive($ic, $lib, $node){
  if (caex215:refsICSimple($ic, $lib, $node))
  then fn:true()
  else (
    let $ics := caex215:getDescendantIC($ic, $lib, root($node)/*)
    for $idx in 1 to count($ics)
    let $cname := caex215:getNameFromPath($ics[$idx])
    let $lname := caex215:getLibFromPath($ics[$idx])
    return caex215:refsICSimple($cname, $lname, $node)
  )
};

declare function caex215:refsIC($ic, $lib, $node){
  if (functx:is-value-in-sequence(fn:true(), caex215:refsICRecursive($ic, $lib, $node)))
  then fn:true()
  else fn:false()
};

declare function caex215:refsRCSimple($rc, $lib, $node){
  if($node[fn:starts-with(@RefBaseClassPath, $lib) and fn:ends-with(@RefBaseClassPath, $rc)])
  then fn:true()
  else fn:false()
};

declare function caex215:refsRCRecursive($rc, $lib, $node){
  if (caex215:refsRCSimple($rc, $lib, $node))
  then fn:true()
  else (
    let $rcs := caex215:getDescendantRC($rc, $lib, root($node)/*)
    for $idx in 1 to count($rcs)
    let $cname := caex215:getNameFromPath($rcs[$idx])
    let $lname := caex215:getLibFromPath($rcs[$idx])
    return caex215:refsRCSimple($cname, $lname, $node)
  )
};

declare function caex215:refsRC($rc, $lib, $node){
  if (functx:is-value-in-sequence(fn:true(), caex215:refsICRecursive($rc, $lib, $node)))
  then fn:true()
  else fn:false()
};

(:check whether the given node matches any node in the references
  - name
  - attribute names
:)
declare function caex215:matches($node as node()?, $references as node()*){
  some $reference in $references satisfies ($reference/name() = $node/name()) 
  and (
    for $rAttr in $reference/@*
    return some $nAttr in $node/@* satisfies $rAttr
    (:return exists($node/@*[name() = $rAttr/name()]):)
  )  
  and (
    for $cRef in $reference/*
    return
      caex215:matches($cRef, $node/*)
  )
};

(:check whether the given node matches any node in the references
  - name
  - attrbute names
  - sub nodes
:)
declare function caex215:matches-deep($node as node()?, $references as node()*){
  if(caex215:matches($node, $references))
  then(
    for $cRef in $references/*
    return
      some $cNode in $node/* satisfies $cRef
  )
  else fn:false()
};

(:filter out all objects in the reference list in a caex object:)
declare function caex215:filter($node as node()?, $references as node()*){
  for $child in $node/*
  return (
    $child/@Name,
    caex215:matches($child, $references)
  )
};