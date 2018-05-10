// =========================================================================
// Copyright 2018 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================

package com.tmobile.cso.vault.api.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.model.SafeNode;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;

import io.swagger.annotations.Api;


@RestController
@CrossOrigin
@Api(description = "Manage Secrets", position = 5)
public class SecretController {
	
	@Autowired
	private RequestProcessor reqProcessor;
	

	@GetMapping(value="/read",produces= "application/json")
	public ResponseEntity<String> readFromVault(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		
		// Path would be apps|shared|users/<safename>/<foldename
		if(ControllerUtil.isValidDataPath(path)){
			//if(ControllerUtil.isValidSafe(path,token)){
				Response response = reqProcessor.process("/read","{\"path\":\""+path+"\"}",token);
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			//}else{
			//	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid safe\"]}");
			//}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
		}
			
	}
	@PostMapping(value="/write",consumes="application/json",produces="application/json")
	public ResponseEntity<String> writeToVault(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
		
		String path="";
		try {
			path = new ObjectMapper().readTree(jsonStr).at("/path").asText();
			jsonStr = ControllerUtil.addDefaultSecretKey(jsonStr);
			if (!ControllerUtil.isSecretKeyValid(jsonStr)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request.Check json data\"]}");
			}
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request.Check json data\"]}");
		}
		if(ControllerUtil.isValidDataPath(path)){
			//if(ControllerUtil.isValidSafe(path,token)){
				Response response = reqProcessor.process("/write",jsonStr,token);
				if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret saved to vault\"]}");
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			//}else{
			//	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid safe\"]}");
			//}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
		}
	}
	
//	@PostMapping(value="/v2/write",consumes="application/json",produces="application/json")
//	public ResponseEntity<String> write(@RequestHeader(value="vault-token") String token, @RequestBody String jsonStr){
//		
//		String path="";
//		try {
//			path = new ObjectMapper().readTree(jsonStr).at("/path").asText();
//			if (!ControllerUtil.isSecretKeyValid(jsonStr)) {
//				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request.Check json data\"]}");
//			}
//		} catch (IOException e) {
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid request.Check json data\"]}");
//		}
//		if(ControllerUtil.isPathValid(path)){
//			//if(ControllerUtil.isValidSafe(path,token)){
//				Response response = reqProcessor.process("/write",jsonStr,token);
//				if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
//					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret saved to vault\"]}");
//				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
//			//}else{
//			//	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid safe\"]}");
//			//}
//		}else{
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
//		}
//	}
	
	
	
	@DeleteMapping(value="/delete",produces="application/json")
	public ResponseEntity<String> deleteFromVault(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		if(ControllerUtil.isValidDataPath(path)){
			//if(ControllerUtil.isValidSafe(path,token)){
				Response response = reqProcessor.process("/delete","{\"path\":\""+path+"\"}",token);
				if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT))
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secrets deleted\"]}");
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			//}else{
			//	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid safe\"]}");
			//}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
		}
	}
	
	/**
	 * Reads the given sdb path/folder recursively
	 * @param token
	 * @param path
	 * @return
	 */
	@GetMapping(value="/readfull",produces= "application/json")
	public ResponseEntity<String> readFromVaultRecursive(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		Response response = new Response(); 
		SafeNode safeNode = new SafeNode();
		safeNode.setId(path);
		if (ControllerUtil.isValidSafePath(path)) {
			safeNode.setType("safe");			
		}
		else {
			safeNode.setType("folder");
		}
		ControllerUtil.recursiveRead("{\"path\":\""+path+"\"}",token,response, safeNode);
		ObjectMapper mapper = new ObjectMapper();
		try {
			String res = mapper.writeValueAsString(safeNode);
			return ResponseEntity.status(response.getHttpstatus()).body(res);
		} catch (JsonProcessingException e) {
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	
	/**
	 * Reads the contents of a safe/folder, includes both folders and secrets
	 * @param token
	 * @param path
	 * @return
	 */
	@GetMapping(value="/readAll",produces= "application/json")
	public ResponseEntity<String> readFoldersAndSecrets(@RequestHeader(value="vault-token") String token, @RequestParam("path") String path){
		Response response = new Response(); 
		SafeNode safeNode = new SafeNode();
		safeNode.setId(path);
		if (ControllerUtil.isValidSafePath(path)) {
			safeNode.setType("safe");			
		}
		else {
			safeNode.setType("folder");
		}
		ControllerUtil.getFoldersAndSecrets("{\"path\":\""+path+"\"}",token,response, safeNode);
		ObjectMapper mapper = new ObjectMapper();
		try {
			String res = mapper.writeValueAsString(safeNode);
			return ResponseEntity.status(response.getHttpstatus()).body(res);
		} catch (JsonProcessingException e) {
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
}

