module namespace caex215 = "http://ipr.kit.edu/caex";
import module namespace functx = "http://www.functx.com" at "./functx-1.0.xqy";
(:import module namespace basics = "http://ipr.kit.edu/xquery/basics" at "/Users/aris/Documents/repositories/ipr/aml_tools/resources/xquery/basics.xqy";:)

(:==============================================================================
: Part 1 - Helper functions to handle the syntax of CAEX including
: - caex215:isLib($context)
: - caex215:getLib($context)
: - caex215:getLibFromPath($path as xs:string)
: - caex215:getNameFromPath($path as xs:string)
: - caex215:getCAEXClassPath($node)
: - caex215:matchAttribute($attribute_name as xs:string, $value as xs:string, $context as node())
: - caex215:matchAttributeRecursive($attribute_name as xs:string, $value as xs:string, $context as node())
: - caex215:parseRefSide($refSide as xs:string, $context as node())
==============================================================================:)


(:~
: Check if the obj node is a CAEX library object
: 
: @return	true/false
: @param	$obj the obj node being evaluated
:)
declare function caex215:isLib($obj as node())  {
  if ($obj[self::InterfaceClassLib] or $obj[self::RoleClassLib] or $obj[self::SystemUnitClassLib] or $obj[self::InstanceHierarchy])
  then fn:true()
  else fn:false()
};



(:~
: Get the CAEX library object of the obj node
: 
: @return	the CAEX library object
: @param	$obj the obj node being evaluated
:)
declare function caex215:getLib($obj as node()) as node() {
  $obj/ancestor-or-self::*[caex215:isLib(.)]
};



(:~
: Get the CAEX library name of from a CAEX class path
: A CAEX class path has the form: libname/.../classname
: 
: @return	the CAEX library name
: @param	$path the CAEX class path
:)
declare function caex215:getLibFromPath($path as xs:string) as xs:string {
  fn:substring-before($path, "/")
};



(:~
: Get the CAEX class name of from a CAEX class path
: A CAEX class path has the form: libname/.../classname
: 
: @return	the CAEX class name
: @param	$path the CAEX class path
:)
declare function caex215:getNameFromPath($path as xs:string) as xs:string {
  functx:substring-after-last($path, "/")
};



(:~
: Get the CAEX class path from a CAEX class obj
: A CAEX class path has the form: libname/.../classname
: 
: @return	the CAEX class path
: @param	$obj the CAEX class obj
:)
declare function caex215:getCAEXClassPath($obj as node()) as xs:string{
  let $ref := string($obj/@RefBaseClassPath)
  let $lib := string(caex215:getLib($obj)/@Name)
  let $name := string($obj/@Name)
  return
    if(caex215:isLib($obj))
    then $obj/@Name
    else
      if($ref)
      then
        let $reflib := caex215:getLibFromPath($ref) 
        let $refparent := caex215:getNameFromPath($ref)
        return
          if($reflib = $lib)
          then fn:concat($ref, "/", $name)
          else if ($reflib = string($obj/parent::self/@Name))
          then fn:concat(caex215:getCAEXClassPath($obj/..), "/", $name)
          else fn:concat(caex215:getCAEXClassPath($obj/..), "/", $name)(:fn:concat($lib, "/", $name):)
      else
        fn:concat($lib, "/", $name)
};



(:~
: Check if a CAEX attribute has a specific value
: The value of a CAEX attribute is a sub XML text element
: 
: @return	true/false
: @param	$attribute_name the name of the attribute
: @param	$value the desired value
: @param	$obj the obj node that contains the attribute
:)
declare function caex215:matchAttribute($attribute_name as xs:string, $value as xs:string, $obj as node()) {
  fn:exists($obj/Attribute[@Name=$attribute_name]/Value[text()=$value])
};



(:~
: Recursively check if a CAEX nested attribute has a specific value
: The value of a CAEX attribute is a sub XML text element
: 
: @return	true/false
: @param	$attribute_name the name of the attribute
: @param	$value the desired value
: @param	$obj the obj node that contains the nested attribute
:)
declare function caex215:matchAttributeRecursive($attribute_name as xs:string, $value as xs:string, $obj as node()) {
	if (caex215:matchAttribute($attribute_name, $value, $obj))
	then fn:true()
	else 
		for $child in $obj/Attribute
		return (caex215:matchAttributeRecursive($attribute_name, $value, $child))
};


(:~
: Get the name and id of the interface and its owner object
: from the given CAEX RefPartnerSide string of a CAEX internal link
: The input is assumed to be AML editor conform (for CAEX 2.15)
: i.e. the RefPartnerSide has the form: [ownerId]:[interfaceName]
: 
: @return	name and id of the interface and its owner object
: @param	$refSide the RefPartnerSide of an internal link
: @param	$context the context node for findig the root of the document
:)
declare function caex215:parseRefSide($refSide as xs:string, $context as node()) as item()* {
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


(: =============================================================================
: Part 2 - Helper functions to traverse the class hierarchy of CAEX including
: - caex215:getChildRC($refbase as xs:string, $root as node())
: - caex215:getDescendantRC($refbase as xs:string, $root as node())
: - caex215:getDescendantRC($role as xs:string, $lib as xs:string, $root as node())
: - caex215:getChildIC($refbase as xs:string, $root as node())
: - caex215:getDescendantIC($refbase as xs:string, $root as node())
: - caex215:getDescendantIC($role as xs:string, $lib as xs:string, $root as node())
============================================================================= :)


(:~
: Get all child role classes of a base role class
: 
: @return	the child role classes
: @param	$refbase the class path of the base role class
: @param	$root the root node of the AML file
:)
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
                  <cpath>{caex215:getCAEXClassPath($candidate)}</cpath>
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
                      let $path := caex215:getCAEXClassPath($xmlparent)
                      return
                          if($path = $refbase)
                          then
                            <child>
                              <cpath>{caex215:getCAEXClassPath($candidate)}</cpath>
                              <ppath>{$refbase}</ppath>
                            </child> 
                          else ()
                else()
       }
        </children>    
};



(:~
: Get all descendant role classes of a base role class
: 
: @return	the descendant role classes
: @param	$refbase the class path of the base role class
: @param	$root the root node of the AML file
:)
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
          return caex215:getDescendantRC(caex215:getCAEXClassPath($n), $root)
       )
};



(:~
: Get all descendant role classes of a base role class
: 
: @return	the descendant role classes
: @param	$roleName the name of the base role class
: @param	$libName the name of the library to which the base role class belongs
: @param	$root the root node of the AML file
:)
declare function caex215:getDescendantRC($roleName as xs:string, $libName as xs:string, $root as node()) as item()* {
  let $node := $root//RoleClass[@Name=$roleName and caex215:getLib(.)/@Name = $libName]
  return caex215:getDescendantRC(caex215:getCAEXClassPath($node), $root)
};



(:~
: Get all child interface classes of a base interface class
: 
: @return	the child interface classes
: @param	$refbase the class path of the base interface class
: @param	$root the root node of the AML file
:)
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
                <cpath>{caex215:getCAEXClassPath($candidate)}</cpath>
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
                    let $path := caex215:getCAEXClassPath($xmlparent)
                    return
                        if($path = $refbase)
                        then
                          <child>
                            <cpath>{caex215:getCAEXClassPath($candidate)}</cpath>
                            <ppath>{$refbase}</ppath>
                          </child> 
                        else ()
              else()
     }
    </children>    
};



(:~
: Get all descendant interface classes of a base interface class
: 
: @return	the descendant interface classes
: @param	$refbase the class path of the base interface class
: @param	$root the root node of the AML file
:)
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
          return caex215:getDescendantIC(caex215:getCAEXClassPath($n), $root)
       )
};



(:~
: Get all descendant interface classes of a base interface class
: 
: @return	the descendant interface classes
: @param	$roleName the name of the base interface class
: @param	$libName the name of the library to which the base interface class belongs
: @param	$root the root node of the AML file
:)
declare function caex215:getDescendantIC($roleName as xs:string, $libName as xs:string, $root as node()) as item()* {
  let $node := $root//InterfaceClass[@Name=$roleName and caex215:getLib(.)/@Name = $libName]
  return caex215:getDescendantIC(caex215:getCAEXClassPath($node), $root)
};



(: =============================================================================
: Part 3 - Transitive Closure
: - caex215:getClosure($context as node(), $test as function(node()) )
============================================================================= :)



(:~
: Get all descendent-or-self of the obj nodes that are from the transitive closure of a pattern 
: the pattern is given as a test function
: 
: @return	the nodes from the transitive closure
: @param	$obj the obj node
: @param	$test a function that describes the pattern
:)
declare function caex215:getClosure($obj as node(), $test as function(node()) as xs:boolean) as node()*{
  if ($test($obj))
  then (
		$obj, 
		for $child in $obj/*
		return caex215:getClosure($child, $test)
	)
	else ()
};



(: =============================================================================
: Part 4 - Functions to check the satisfiability of CAEX objs w.r.t. CAEX classes
: - caex215:getIEOfRR($roleName as xs:string, $libName as xs:string, $context as node())
: - caex215:getObjsOfSRC($roleName, $libName, $context)
:
: - caex215:supportsRCSimple($roleName as xs:string, $libName as xs:string, $obj as node())
: - caex215:supportsRCRecursive($roleName, $libName, $obj)
: - caex215:supportsRC($roleName, $libName, $obj)
:
: - caex215:requiresRRSimple($roleName as xs:string, $libName as xs:string, $obj as node())
: - caex215:requiresRRRecursive($roleName, $libName, $obj)
: - caex215:requiresRR($roleName, $libName, $obj)
:
: - caex215:refsICSimple($icName as xs:string, $libName as xs:string, $node as node())
: - caex215:refsICRecursive($icName as xs:string, $libName as xs:string, $obj as node())
: - caex215:refsIC($icName as xs:string, $libName as xs:string, $obj as node())
:
: - caex215:refsRCSimple($rcName as xs:string, $libName as xs:string, $obj as node())
: - caex215:refsRCRecursive($rcName as xs:string, $libName as xs:string, $obj as node())
: - caex215:refsRC($rcName as xs:string, $libName as xs:string, $obj as node())
============================================================================= :)




(:~
: Get all CAEX IEs w.r.t. the context node that require the given role semantically 
: the class hierarchy of the given role class is supposed to be in the same AML file as the CAEX obj
: 
: @return	the IE nodes that requires the given role or any of its sub classes
: @param	$roleName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$context the context node
:)
declare function caex215:getIEOfRR($roleName as xs:string, $libName as xs:string, $context as node()) as node()* {
  let $rcs := caex215:getDescendantRC($roleName, $libName, root($context))
  for $node in $context/descendant-or-self::*
  return (	
    if($node/RoleRequirements[fn:starts-with(@RefBaseRoleClassPath, $libName) and fn:ends-with(@RefBaseRoleClassPath, $roleName)])
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



(:~
: Get all CAEX objects (IE, SUC) w.r.t. the context node that support the given role semantically
: the class hierarchy of the given role class is supposed to be in the same AML file as the CAEX obj
: 
: @return	the CAEX object nodes that support the given role or any of its sub classes
: @param	$roleName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$context the context node
:)
declare function caex215:getObjsOfSRC($roleName as xs:string, $libName as xs:string, $context as node()) as node()* {
  let $rcs := caex215:getDescendantRC($roleName, $libName, root($context))
  for $node in $context/descendant-or-self::*
  return (	
    if($node/SupportedRoleClass[fn:starts-with(@RefRoleClassPath, $libName) and fn:ends-with(@RefRoleClassPath, $roleName)])
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



(:~
: Check if the given CAEX obj supports the given role syntactically
: 
: @return	true/false
: @param	$roleName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:supportsRCSimple($roleName as xs:string, $libName as xs:string, $obj as node())  {
  if($obj/SupportedRoleClass[fn:starts-with(@RefRoleClassPath, $libName) and fn:ends-with(@RefRoleClassPath, $roleName)])
  then fn:true()
  else fn:false()
};



(:~
: Recursively check if the given CAEX obj supports the given role semantically
: inner function, shall not be used directly
: the class hierarchy of the given role class is supposed to be in the same AML file as the CAEX obj
:
: @return	true/false for each of the descendant-or-self of the role class
: @param	$roleName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$obj the obj to be evaluated
:)
declare function caex215:supportsRCRecursive($roleName as xs:string, $libName as xs:string, $obj as node()) {
  if (caex215:supportsRCSimple($roleName, $libName, $obj))
  then fn:true()
  else (
    let $rcs := caex215:getDescendantRC($roleName, $libName, root($obj)/*)
    for $idx in 1 to count($rcs)
    let $cname := caex215:getNameFromPath($rcs[$idx])
    let $lname := caex215:getLibFromPath($rcs[$idx])
    return caex215:supportsRCSimple($cname, $lname, $obj)
  )
};


(:~
: Recursively check if the given CAEX obj supports the given role semantically
: the class hierarchy of the given role class is supposed to be in the same AML file as the CAEX obj
: 
: @return	true/false
: @param	$roleName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:supportsRC($roleName as xs:string, $libName as xs:string, $obj as node()) {
  if (functx:is-value-in-sequence(fn:true(), caex215:supportsRCRecursive($roleName, $libName, $obj)))
  then fn:true()
  else fn:false()
};



(:~
: Check if the given CAEX obj requires the given role syntactically
: 
: @return	true/false
: @param	$roleName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:requiresRRSimple($roleName as xs:string, $libName as xs:string, $obj as node())  {
  (:if($node/RoleRequirements[fn:starts-with(@RefBaseRoleClassPath, $lib) and fn:ends-with(@RefBaseRoleClassPath, $role)]):)
  (:assume the node is the RR itself:)
  if($obj[fn:starts-with(@RefBaseRoleClassPath, $libName) and fn:ends-with(@RefBaseRoleClassPath, $roleName)])
  then fn:true()
  else fn:false()
};



(:~
: Recursively check if the given CAEX obj requires the given role semantically
: inner function, shall not be used directly
: the class hierarchy of the given role class is supposed to be in the same AML file as the CAEX obj
: 
: @return	true/false for each of the descendant-or-self of the role class
: @param	$roleName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:requiresRRRecursive($roleName as xs:string, $libName as xs:string, $obj as node()) {
  if (caex215:requiresRRSimple($roleName, $libName, $obj))
  then fn:true()
  else (
    let $rcs := caex215:getDescendantRC($roleName, $libName, root($obj)/*)
    for $idx in 1 to count($rcs)
    let $cname := caex215:getNameFromPath($rcs[$idx])
    let $lname := caex215:getLibFromPath($rcs[$idx])
    return caex215:requiresRRSimple($cname, $lname, $obj)
  )
};


(:~
: Recursively check if the given CAEX obj requires the given role semantically
: the class hierarchy of the given role class is supposed to be in the same AML file as the CAEX obj
: 
: @return	true/false
: @param	$roleName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:requiresRR($roleName as xs:string, $libName as xs:string, $obj as node())  {
  if (functx:is-value-in-sequence(fn:true(), caex215:requiresRRRecursive($roleName, $libName, $obj)))
  then fn:true()
  else fn:false()
};



(:~
: Check if the given CAEX obj refers to the given interface class syntactically
: 
: @return	true/false
: @param	$icName the name of the interface class
: @param	$libName the name of the libray to which the interface class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:refsICSimple($icName as xs:string, $libName as xs:string, $node as node())  {
  if($node[fn:starts-with(@RefBaseClassPath, $libName) and fn:ends-with(@RefBaseClassPath, $icName)])
  then fn:true()
  else fn:false()
};



(:~
: Recusrively check if the given CAEX obj refers to the given interface class semantically
: inner function, shall not be directly used
: the class hierarchy of the given interface class is supposed to be in the same AML file as the CAEX obj
: 
: @return	true/false for each of the descendant-or-self of the interface class
: @param	$icName the name of the interface class
: @param	$libName the name of the libray to which the interface class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:refsICRecursive($icName as xs:string, $libName as xs:string, $obj as node()) {
  if (caex215:refsICSimple($icName, $libName, $obj))
  then fn:true()
  else (
    let $ics := caex215:getDescendantIC($icName, $libName, root($obj)/*)
    for $idx in 1 to count($ics)
    let $cname := caex215:getNameFromPath($ics[$idx])
    let $lname := caex215:getLibFromPath($ics[$idx])
    return caex215:refsICSimple($cname, $lname, $obj)
  )
};



(:~
: Recusrively check if the given CAEX obj refers to the given interface class semantically
: the class hierarchy of the given interface class is supposed to be in the same AML file as the CAEX obj
: 
: @return	true/false 
: @param	$icName the name of the interface class
: @param	$libName the name of the libray to which the interface class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:refsIC($icName as xs:string, $libName as xs:string, $obj as node()) {
  if (functx:is-value-in-sequence(fn:true(), caex215:refsICRecursive($icName, $libName, $obj)))
  then fn:true()
  else fn:false()
};



(:~
: Check if the given CAEX obj refers to the given role class syntactically
: 
: @return	true/false
: @param	$rcName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:refsRCSimple($rcName as xs:string, $libName as xs:string, $obj as node()) {
  if($obj[fn:starts-with(@RefBaseClassPath, $libName) and fn:ends-with(@RefBaseClassPath, $rcName)])
  then fn:true()
  else fn:false()
};



(:~
: Recusrively check if the given CAEX obj refers to the given role class syntactically
: inner function, shall not be used directly
: the class hierarchy of the given role class is supposed to be in the same AML file as the CAEX obj 
:
: @return	true/false for each of the descendant-or-self of the role class
: @param	$rcName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:refsRCRecursive($rcName as xs:string, $libName as xs:string, $obj as node()) {
  if (caex215:refsRCSimple($rcName, $libName, $obj))
  then fn:true()
  else (
    let $rcs := caex215:getDescendantRC($rcName, $libName, root($obj)/*)
    for $idx in 1 to count($rcs)
    let $cname := caex215:getNameFromPath($rcs[$idx])
    let $lname := caex215:getLibFromPath($rcs[$idx])
    return caex215:refsRCSimple($cname, $lname, $obj)
  )
};

(:~
: Recusrively check if the given CAEX obj refers to the given role class syntacticallyå
: the class hierarchy of the given role class is supposed to be in the same AML file as the CAEX obj
: 
: @return	true/false
: @param	$rcName the name of the role class
: @param	$libName the name of the libray to which the role class belongs
: @param	$obj the CAEX obj to be evaluated
:)
declare function caex215:refsRC($rcName as xs:string, $libName as xs:string, $obj as node()) {
  if (functx:is-value-in-sequence(fn:true(), caex215:refsICRecursive($rcName, $libName, $obj)))
  then fn:true()
  else fn:false()
};





(: =============================================================================
: Part 5 - Functions to check the satisfiability of a CAEX object w.r.t links
:
: - caex215:getConnections($interface as node())
: - caex215:getPartnersById($interfaceId as xs:string, $context as node())
: - caex215:getPartnersByRefSide($refside as xs:string, $context as node())
: - caex215:getConnectedInterfacesFromObj($obj as node())
: - caex215:isConnected($interface as node())
: - caex215:isConnected($interfaceId as xs:string, $context as node())
:
: - caex215:connectsTo($interface as node(), $partner as node())
: - caex215:connectsToAny($interface as node(), $candidates as node()*)
: - caex215:connectsToRefSide($interface as node(), $partnerRefSide as xs:string)
: - caex215:connectsToById($interfaceId as xs:string, $partnerId as xs:string, $context as node())
============================================================================= :)



(:~
: Get all CAEX RefPartnerSides which are connected to any interface of the given object via InternalLink
: the obj can be RC, IC, SUC, EI, IE
: for complex objects, i.e. RC, SUC, IE, the function looks for all IDs of the desending nodes 
: and checkts whether any of them is connected
: 
: @return the CAEX RefPartnerSides
: @param $obj the object to be tested
:)
(:declare function caex215:getRefPartnersRecursive($obj as node()) as xs:string*{	
	for $id in $obj/descendant-or-self::*/@ID
	let $links := root($obj)//InternalLink
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
};:)



(:~
: Check if the given CAEX obj is connected to any other interfaces
: the obj can be RC, IC, SUC, EI, IE
: for complex objects, i.e. RC, SUC, IE, the function looks for all IDs of the desending nodes 
: and checkts whether any of them is connected
: 
: @return true/false
: @param $obj the object to be tested
:)
(:declare function caex215:isObjConnected($obj as node())  {
	count(caex215:getRefPartnersRecursive($obj)) > 0
};:)




(:~
: Get all interfaces which are connected to the given interface via InternalLink
: This function traverses all lins in the AML file and find the ones that contain 
: the given interface as a RefPartnerSide
: 
: @return a link object that contains the ID and Name of the partern interfaces and their owner objects
: @param $interface the interface to be tested
:)
declare function caex215:getConnections($interface as node()) as item()* {
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



(:~
: Get the connected interfaces of the given interface node (represented using its ID)
: 
: @return connected interfaces
: @param $interfaceId the id of the interface node to be tested
: @param $context the context node to find the interface node based on its ID
:)
declare function caex215:getPartnersById($interfaceId as xs:string, $context as node()) as node()* {
  let $interface := root($context)//*[@ID=$interfaceId]
  return
    if ($interface)
    then
      let $partnerId := caex215:getConnections($interface)/partner/iid
      return root($context)//*[@ID=$partnerId]
    else
      ()
};



(:~
: Get the connected interfaces of the given interface node (represented using its RefPartnerSide in a link)
: 
: @return connected interfaces
: @param $refside the CAEX RefPartnerSide of the interface node to be tested
: @param $context the context node to find the interface node based on its ID
:)
declare function caex215:getPartnersByRefSide($refside as xs:string, $context as node()) as node()* {
  let $interfaceId := caex215:parseRefSide($refside, $context)/iid
  return caex215:getPartnersById($interfaceId, $context)
};



(:~
: Get all interfaces which are connected to any interface of the given object via InternalLink
: 
: @return a links object that contains all the ID and Name of the partern interfaces and their owner objects
: @param $obj the object to be tested
:)
declare function caex215:getConnectedInterfacesFromObj($obj as node()) as item()* {
  for $interface in $obj/descendant-or-self::ExternalInterface
  return 
    <links>
      {caex215:getConnections($interface)}
    </links>
};


(:~
: Check if the given interface node (represented using its ID) is connected with any nodes
: 
: @return true/false
: @param $interfaceId the id of the interface node to be tested
: @param $context the context node to find the interface node based on its ID
:)
declare function caex215:isConnected($interfaceId as xs:string, $context as node())  {
  let $node := root($context)//*[@ID=$interfaceId]
  return count(caex215:getConnections($node))>0
};


(:~
: Check if the given interface node is connected with any nodes
: 
: @return true/false
: @param $interface the interface node to be tested
:)
declare function caex215:isConnected($interface as node())  {
  count(caex215:getConnections($interface))>0
};



(:~
: Check if the given interface node is connected with the partner node
: 
: @return true/false
: @param $interface the interface node to be tested
: @param $partner the partner node
:)
declare function caex215:connectsTo($interface as node(), $partner as node())  {
  let $links := caex215:getConnections($interface)
  let $pids := $links/partner/iid
  return exists(functx:value-intersect($pids, $partner/@ID))
};



(:~
: Check if the given interface node is connected with any of the given candidate nodes
: 
: @return true/false
: @param $interface the interface node to be tested
: @param $candidates the candidate nodes
:)
declare function caex215:connectsToAny($interface as node(), $candidates as node()*) {
  let $links := caex215:getConnections($interface)
  (:use value-intersect to find IDs that appear in both the real parterns and the required partners:)
  return exists(functx:value-intersect($links/partner/iid, $candidates/@ID))
};



(:~
: Check if the given interface is connected to the given partner (represented using RefPartnerSide)
: 
: @return true/false
: @param $interfaceId ID of the interface to be tested
: @param $partnerRefSide RefPartnerSide of the partner to be tested
: @param $context the context node for finding the interfaces  
:)
declare function caex215:connectsToRefSide($interface as node(), $partnerRefSide as xs:string)  {
  let $pid := caex215:parseRefSide($partnerRefSide, root($interface))/iid
  return functx:is-node-in-sequence($interface, caex215:getPartnersById($pid, root($interface)))
};



(:~
: Check if the given interface is connected to the given partner by Id
: 
: @return true/false
: @param $interfaceId ID of the interface to be tested
: @param $partnerId ID of the partner to be tested
: @param $context the context node for finding the interfaces  
:)
declare function caex215:connectsToById($interfaceId as xs:string, $partnerId as xs:string, $context as node())  {
  let $interface := root($context)//*[@ID=$interfaceId]
  let $partner := root($context)//*[@ID=$partnerId]
  return
    if (fn:exists($interface) and fn:exists($partner))
    then 
      caex215:connectsTo($interface, $partner)
    else
      fn:false()
};

