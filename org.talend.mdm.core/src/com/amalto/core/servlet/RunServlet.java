package com.amalto.core.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amalto.core.objects.configurationinfo.assemble.AssembleConcreteBuilder;
import com.amalto.core.objects.configurationinfo.assemble.AssembleDirector;
import com.amalto.core.objects.configurationinfo.assemble.AssembleProc;
import com.amalto.core.util.Util;


/**
 * @author bgrieder
 * 
 *
 */

public class RunServlet extends HttpServlet {


    
    /**
     * Constructor
     * 
     */
    public RunServlet() {
        super();
    }

    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
    }

    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
            doGet(request,response);
        }
    

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException,
        IOException {

        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
//        String parameters = req.getParameter("parameters");
        

        resp.setContentType("text/html; charset=\"UTF-8\"");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter writer = resp.getWriter();
        writer.write("<html><body>");
        
    	try {
    		if ("clean".equals(action)) {
    			writer.write(
   					"<p><b>Cleanup the background jobs stored in MDM</b><br/>" +
   					"Check jboss/server/default/log/server.log or the jboss console output to determine when clean is completed</b></p>"
    			);
    			
    			final AssembleConcreteBuilder concreteBuilder = new AssembleConcreteBuilder();
				final AssembleDirector director = new AssembleDirector(concreteBuilder);
				director.constructCleanPart();
				AssembleProc assembleProc = concreteBuilder.getAssembleProc();
				
    			Util.getConfigurationInfoCtrlLocal().autoUpgradeInBackground(assembleProc);
    			
    		} else if("init".equals(action)){
    			writer.write(
       					"<p><b>Initialize the clusters and build-in data</b><br/>" +
       					"Check jboss/server/default/log/server.log or the jboss console output to determine when init is completed</b></p>"
        			);
        			
        		final AssembleConcreteBuilder concreteBuilder = new AssembleConcreteBuilder();
    			final AssembleDirector director = new AssembleDirector(concreteBuilder);
    			director.constructInitPart();
    			AssembleProc assembleProc = concreteBuilder.getAssembleProc();
    				
        		Util.getConfigurationInfoCtrlLocal().autoUpgradeInBackground(assembleProc);
    			
    		} else if("migrate".equals(action)){
    			writer.write(
       					"<p><b>Migrate data to the new revision</b><br/>" +
       					"Check jboss/server/default/log/server.log or the jboss console output to determine when migrate is completed</b></p>"
        			);
        			
        		final AssembleConcreteBuilder concreteBuilder = new AssembleConcreteBuilder();
    			final AssembleDirector director = new AssembleDirector(concreteBuilder);
    			director.constructMigratePart();
    			AssembleProc assembleProc = concreteBuilder.getAssembleProc();
    				
        		Util.getConfigurationInfoCtrlLocal().autoUpgradeInBackground(assembleProc);
    			
    		} else {
    			writer.write(
   					"<p><b>Unknown action: </b>"+action+"<br/>"
    			);
    			
    		}
    	
    	} catch (Exception e) {
    		writer.write("<h1>An error occured: "+e.getLocalizedMessage()+"</h1>");
    	}
        writer.write("</body></html>");
                
    }
    
      
 
 
    
}