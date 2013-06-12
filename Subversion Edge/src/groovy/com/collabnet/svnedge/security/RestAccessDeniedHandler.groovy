package com.collabnet.svnedge.security

import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.AccessDeniedException
import org.springframework.security.ui.AccessDeniedHandler
import javax.servlet.http.HttpServletRequest
import grails.converters.JSON
import grails.converters.XML

/**
 * Spring Security AccessDeniedHandler that sends 401 instead of redirecting to the login denied page
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler  {

  def messageSource

  @Override
  void handle(ServletRequest servletRequest, ServletResponse servletResponse, AccessDeniedException e) {
      def response = servletResponse as HttpServletResponse
      def request = servletRequest as HttpServletRequest
      String format = request.getParameter("format")
      if (!format) {
          if (request.contentType.contains("json")) {
              format = "json"
          }
          else {
              format = "xml"
          }
      }
      def resultMap = [:]
      resultMap["errorMessage"] = messageSource.getMessage("api.error.401", null, request.locale)
      def result = (format == "xml") ? resultMap as XML : resultMap as JSON        

      response.status = 401
      response.writer.write(result.toString())
  }
}
