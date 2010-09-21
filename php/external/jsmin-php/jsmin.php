<?php
/**
 * jsmin.php - PHP implementation of Douglas Crockford's JSMin.
 *
 * This is pretty much a direct port of jsmin.c to PHP with just a few
 * PHP-specific performance tweaks. Also, whereas jsmin.c reads from stdin and
 * outputs to stdout, this library accepts a string as input and returns another
 * string as output.
 *
 * PHP 5 or higher is required.
 *
 * Permission is hereby granted to use this version of the library under the
 * same terms as jsmin.c, which has the following license:
 *
 * --
 * Copyright (c) 2002 Douglas Crockford  (www.crockford.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * The Software shall be used for Good, not Evil.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * --
 *
 * @package JSMin
 * @author Ryan Grove <ryan@wonko.com>
 * @copyright 2002 Douglas Crockford <douglas@crockford.com> (jsmin.c)
 * @copyright 2008 Ryan Grove <ryan@wonko.com> (PHP port)
 * @license http://opensource.org/licenses/mit-license.php MIT License
 * @version 1.1.1 (2008-03-02)
 * @link http://code.google.com/p/jsmin-php/
 */

class JSMin {
  const ORD_LF    = 10;
  const ORD_SPACE = 32;

  private $a           = '';
  private $b           = '';
  private $input       = '';
  private $inputIndex  = 0;
  private $inputLength = 0;
  private $lookAhead   = null;
  private $output      = '';

  // -- Public Static Methods --------------------------------------------------

  public static function minify($js) {
    $jsmin = new JSMin($js);
    return $jsmin->min();
  }

  // -- Public Instance Methods ------------------------------------------------

  public function __construct($input) {
    $this->input       = str_replace("\r\n", "\n", $input);
    if (strpos($this->input, "\r")) {
      $this->input = str_replace("\r", "\n", $this->input);
    }
    $this->inputLength = strlen($this->input);
  }

  // -- Protected Instance Methods ---------------------------------------------

  private function action($d) {
    switch($d) {
      case 1:
        $this->output .= $this->a;

      case 2:
        $this->a = $this->b;

        if ($this->a === "'" || $this->a === '"') {
          for (;;) {
            $this->output .= $this->a;
            $this->a       = $this->get();

            if ($this->a === $this->b) {
              break;
            }

            if (ord($this->a) <= self::ORD_LF) {
              throw new JSMinException('Unterminated string literal.');
            }

            if ($this->a === '\\') {
              $this->output .= $this->a;
              $this->a       = $this->get();
            }
          }
        }

      case 3:
        $this->b = $this->next();
        $a = $this->a;

        if ($this->b === '/' && (
            $a === '(' || $a === ',' || $a === '=' ||
            $a === ':' || $a === '[' || $a === '!' ||
            $a === '&' || $a === '|' || $a === '?')) {

          $this->output .= $a . $this->b;

          for (;;) {
            $this->a = $this->get();

            if ($this->a === '/') {
              break;
            } elseif ($this->a === '\\') {
              $this->output .= $this->a;
              $this->a       = $this->get();
            } elseif (ord($this->a) <= self::ORD_LF) {
              throw new JSMinException('Unterminated regular expression '.
                  'literal.');
            }

            $this->output .= $this->a;
          }

          $this->b = $this->next();
        }
    }
  }

  private function getLF() {
    for (;;) {
      $c = $this->lookAhead;
      $this->lookAhead = null;

      if ($c === null) {
        $c = ($this->inputIndex < $this->inputLength) ?
          $this->input{$this->inputIndex++} : null;
      }

      $newval = ($c === null || $c === "\n" || ord($c) >= self::ORD_SPACE) ? $c : ' ';

      if (ord($newval) <= self::ORD_LF) {
        return $newval;
      }
    }
  }

  private function getCommentEnd() {
    for (;;) {
      $c = $this->lookAhead;
      $this->lookAhead = null;

      if ($c === null) {
        $c = ($this->inputIndex < $this->inputLength) ?
          $this->input{$this->inputIndex++} : null;
      }

      $newval = ($c === null || $c === "\n" || ord($c) >= self::ORD_SPACE) ? $c : ' ';

      switch ($newval) {
      case '*':
        if ($this->peek() === '/') {
          $this->get();
          return ' ';
        }
        break;
      case null:
        throw new JSMinException('Unterminated comment.');
      }
    }
  }

  private function get() {
    if ($this->lookAhead === null) {
      $c = ($this->inputIndex < $this->inputLength) ?
        $this->input{$this->inputIndex++} : null;
    } else {
      $c = $this->lookAhead;
      $this->lookAhead = null;
    }

    return ($c === null || $c === "\n" || ord($c) >= self::ORD_SPACE) ? $c : ' ';
  }

  private function isAlphaNum($c) {
    return  $c === '\\' || ctype_alnum($c) || ord($c) > 126;
  }

  private function min() {
    $this->a = "\n";
    $this->action(3);

    while ($this->a !== null) {
      switch ($this->a) {
        case ' ':
          if (self::isAlphaNum($this->b)) {
            $this->action(1);
          } else {
            $this->action(2);
          }
          break;

        case "\n":
          switch ($this->b) {
            case ' ':
              $this->action(3);
              break;

            case '{':
            case '[':
            case '(':
            case '+':
            case '-':
              $this->action(1);
              break;


            default:
              if (self::isAlphaNum($this->b)) {
                $this->action(1);
              }
              else {
                $this->action(2);
              }
          }
          break;

        default:
          switch ($this->b) {
            case ' ':
              if (self::isAlphaNum($this->a)) {
                $this->action(1);
                break;
              }

              $this->action(3);
              break;

            case "\n":
              switch ($this->a) {
                case '}':
                case ']':
                case ')':
                case '+':
                case '-':
                case '"':
                case "'":
                  $this->action(1);
                  break;

                default:
                  if (self::isAlphaNum($this->a)) {
                    $this->action(1);
                  }
                  else {
                    $this->action(3);
                  }
              }
              break;

            default:
              $this->action(1);
              break;
          }
      }
    }

    return $this->output;
  }

  private function next() {
    $c = $this->get();

    if ($c === '/') {
      switch($this->peek()) {
        case '/':
          return $this->getLF();

        case '*':
          $this->get();
          return $this->getCommentEnd();

        default:
          return $c;
      }
    }

    return $c;
  }

  private function peek() {
    return $this->lookAhead = $this->get();
  }
}

// -- Exceptions ---------------------------------------------------------------
class JSMinException extends Exception {}
?>
