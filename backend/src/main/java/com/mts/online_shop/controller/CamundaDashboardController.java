package com.mts.online_shop.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/camunda")
public class CamundaDashboardController {

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> dashboard() {
        String html = """
                <!doctype html>
                <html><head><meta charset="utf-8"><title>Camunda Dashboard</title></head><body>
                <h1>Camunda & Application Dashboard</h1>
                <p>Preconfigured test user: <strong>admin</strong> / <strong>admin</strong></p>
                <h2>Camunda Engine REST</h2>
                <ul>
                  <li><a href="/engine-rest/engine">/engine-rest/engine</a></li>
                  <li><a href="/engine-rest/process-definition">/engine-rest/process-definition</a></li>
                  <li><a href="/engine-rest/process-instance">/engine-rest/process-instance</a></li>
                  <li><a href="/engine-rest/task">/engine-rest/task</a></li>
                  <li><a href="/engine-rest/history/process-instance?processDefinitionKey=order_process">History for order_process</a></li>
                </ul>
                <h2>Application API (examples)</h2>
                <ul>
                  <li><a href="/api/products">/api/products</a> (products list)</li>
                  <li><a href="/api/orders">/api/orders</a> (orders)</li>
                  <li><a href="/api/auth/login">/api/auth/login</a> (auth)</li>
                </ul>
                <h2>Useful curl examples</h2>
                <pre>curl -u admin:admin http://localhost:8080/engine-rest/process-definition</pre>
                <pre>curl -u admin:admin -H 'Content-Type: application/json' -d '{"variables":{"userId":{"value":2,"type":"Long"}}}' http://localhost:8080/engine-rest/process-definition/key/order_process/start</pre>
                <p>Note: this dashboard is for local testing only.</p>
                </body></html>
                """;
        return ResponseEntity.ok(html);
    }

    @GetMapping(value = "/workbench", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> workbench() {
        String html = """
                <!doctype html>
                <html><head><meta charset="utf-8"><title>Camunda Workbench</title>
                <style>body{font-family:Arial;margin:20px}button{margin:4px}</style>
                </head><body>
                <h1>Camunda Workbench</h1>
                <div>Process key: <input id="procKey" value="order_process"/></div>
                <div><button onclick="startProcess()">Start Process</button></div>
                <h2>Tasks</h2>
                <div>Assignee filter: <input id="assignee" value=""/> <button onclick="loadTasks()">Load</button></div>
                <div id="tasks">(no tasks)</div>
                <script>
                const apiBase='/api/camunda-work';
                async function startProcess(){
                  const key=document.getElementById('procKey').value;
                  const resp=await fetch(apiBase+'/start?key='+encodeURIComponent(key),{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({})});
                  alert('started: '+await resp.text());loadTasks();
                }
                async function loadTasks(){
                  const ass=document.getElementById('assignee').value;
                  const url=apiBase+'/tasks'+(ass?('?assignee='+encodeURIComponent(ass)): '');
                  const r=await fetch(url);const tasks=await r.json();
                  const el=document.getElementById('tasks');el.innerHTML='';
                  if(tasks.length===0){el.innerText='(no tasks)';return;}
                  tasks.forEach(t=>{
                    const d=document.createElement('div');
                    d.style.border='1px solid #ddd';d.style.padding='8px';d.style.margin='6px';
                    const title=document.createElement('div'); title.innerText = t.name + ' (id:' + t.id + ')'; d.appendChild(title);
                    const meta=document.createElement('div'); meta.innerText = 'assignee: ' + (t.assignee || '-'); d.appendChild(meta);
                    const btnClaim=document.createElement('button'); btnClaim.innerText='Claim'; btnClaim.onclick=()=>claim(t.id); d.appendChild(btnClaim);
                    const btnComplete=document.createElement('button'); btnComplete.innerText='Complete'; btnComplete.onclick=()=>complete(t.id); d.appendChild(btnComplete);
                    const btnForm=document.createElement('button'); btnForm.innerText='Form'; btnForm.onclick=()=>showForm(t.id); d.appendChild(btnForm);
                    el.appendChild(d);
                  });
                }
                async function claim(id){await fetch(apiBase+'/task/'+id+'/claim?user=alisa',{method:'POST'});loadTasks();}
                async function complete(id){const vars=prompt('Enter JSON variables or leave empty');let body=null;try{body=vars?JSON.parse(vars):{};}catch(e){alert('invalid json');return;}await fetch(apiBase+'/task/'+id+'/complete',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});loadTasks();}
                async function showForm(id){const r=await fetch(apiBase+'/task/'+id+'/form-variables');const f=await r.json();alert(JSON.stringify(f,null,2));}
                </script>
                </body></html>
                """;
        return ResponseEntity.ok(html);
    }
}
