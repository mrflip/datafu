#!/usr/bin/env ruby
require 'webrick'

WEBrick::HTTPServer.new(:Port => 8000, :DocumentRoot => Dir.pwd).start
