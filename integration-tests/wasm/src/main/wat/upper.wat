;;
;; Licensed to the Apache Software Foundation (ASF) under one or more
;; contributor license agreements.  See the NOTICE file distributed with
;; this work for additional information regarding copyright ownership.
;; The ASF licenses this file to You under the Apache License, Version 2.0
;; (the "License"); you may not use this file except in compliance with
;; the License.  You may obtain a copy of the License at
;;
;;      http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;

;; Copies its standard input to its standard output, with the ASCII letters in upper case
(module
  (import "wasi_snapshot_preview1" "fd_read" (func $fd_read (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (memory (export "memory") 1)

  ;; The I/O vector is at address 0, the number of bytes read or written at address 8 and the buffer at address 1024
  (func (export "_start")
    (local $n i32) (local $i i32) (local $c i32)
    (i32.store (i32.const 0) (i32.const 1024))
    (block $done
      (loop $more
        (i32.store (i32.const 4) (i32.const 4096))
        (if (call $fd_read (i32.const 0) (i32.const 0) (i32.const 1) (i32.const 8))
          (then unreachable))
        (local.set $n (i32.load (i32.const 8)))
        (br_if $done (i32.eqz (local.get $n)))
        (local.set $i (i32.const 0))
        (block $converted
          (loop $each
            (br_if $converted (i32.ge_u (local.get $i) (local.get $n)))
            (local.set $c (i32.load8_u (i32.add (i32.const 1024) (local.get $i))))
            (if (i32.and (i32.ge_u (local.get $c) (i32.const 97)) (i32.le_u (local.get $c) (i32.const 122)))
              (then (i32.store8 (i32.add (i32.const 1024) (local.get $i)) (i32.sub (local.get $c) (i32.const 32)))))
            (local.set $i (i32.add (local.get $i) (i32.const 1)))
            (br $each)))
        (i32.store (i32.const 4) (local.get $n))
        (if (call $fd_write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 8))
          (then unreachable))
        (br $more)))))
