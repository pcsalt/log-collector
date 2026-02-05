package com.pcsalt.logcollector.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class ApiKeyAuthFilter(
  private val validApiKey: String
) : OncePerRequestFilter() {

  companion object {
    const val API_KEY_HEADER = "X-API-Key"
  }

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val apiKey = request.getHeader(API_KEY_HEADER)

    if (apiKey != null && apiKey == validApiKey) {
      val authentication = UsernamePasswordAuthenticationToken(
        "api-client",
        null,
        listOf(SimpleGrantedAuthority("ROLE_API"))
      )
      SecurityContextHolder.getContext().authentication = authentication
    }

    filterChain.doFilter(request, response)
  }
}
